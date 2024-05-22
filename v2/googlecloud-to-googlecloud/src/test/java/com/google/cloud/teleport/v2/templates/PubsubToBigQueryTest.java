/*
 * Copyright (C) 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.cloud.teleport.v2.templates;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.api.services.bigquery.model.TableRow;
import com.google.cloud.teleport.v2.coders.FailsafeElementCoder;
import com.google.cloud.teleport.v2.templates.PubSubToBigQuery.PubsubMessageToTableRow;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.apache.beam.sdk.coders.CoderRegistry;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessageWithAttributesAndMessageIdCoder;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TimestampedValue;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Rule;
import org.junit.Test;

/** Test cases for the {@link PubSubToBigQuery} class. */
public class PubsubToBigQueryTest {

  @Rule public final transient TestPipeline pipeline = TestPipeline.create();

  private static final String RESOURCES_DIR = "JavascriptTextTransformerTest/";

  private static final String TRANSFORM_FILE_PATH =
      Resources.getResource(RESOURCES_DIR + "transform.js").getPath();

  private static final DateTimeFormatter ISO_DATETIME_FORMAT = ISODateTimeFormat.dateTime();

  /** Tests the {@link PubSubToBigQuery} pipeline end-to-end. */
  @Test
  public void testPubsubToBigQueryE2E() throws Exception {
    // Test input
    final String payload = "{\"ticker\": \"GOOGL\", \"price\": 1006.94}";
    final String messageId = "a57809b5-5f57-4b55-8a69-6b22160679de";
    final String topic = "projects/project-id/topics/topic-id";
    final PubsubMessage message =
        new PubsubMessage(
            payload.getBytes(), ImmutableMap.of("id", "123", "type", "custom_event"), messageId);

    final Instant timestamp =
        new DateTime(2022, 2, 22, 22, 22, 22, 222, DateTimeZone.UTC).toInstant();
    final String publishTime = ISO_DATETIME_FORMAT.print(timestamp);
    assert publishTime.equals("2022-02-22T22:22:22.222Z");

    final FailsafeElementCoder<PubsubMessage, String> coder =
        FailsafeElementCoder.of(
            PubsubMessageWithAttributesAndMessageIdCoder.of(), StringUtf8Coder.of());

    CoderRegistry coderRegistry = pipeline.getCoderRegistry();
    coderRegistry.registerCoderForType(coder.getEncodedTypeDescriptor(), coder);

    // Parameters
    PubSubToBigQuery.Options options =
        PipelineOptionsFactory.create().as(PubSubToBigQuery.Options.class);
    options.setJavascriptTextTransformGcsPath(TRANSFORM_FILE_PATH);
    options.setJavascriptTextTransformFunctionName("transform");

    // Build pipeline
    PCollectionTuple transformOut =
        pipeline
            .apply(
                "CreateInput",
                Create.timestamped(TimestampedValue.of(message, timestamp))
                    .withCoder(PubsubMessageWithAttributesAndMessageIdCoder.of()))
            .apply("ConvertMessageToTableRow", new PubsubMessageToTableRow(options));

    // Assert
    PAssert.that(transformOut.get(PubSubToBigQuery.UDF_DEADLETTER_OUT)).empty();
    PAssert.that(transformOut.get(PubSubToBigQuery.TRANSFORM_DEADLETTER_OUT)).empty();
    PAssert.that(transformOut.get(PubSubToBigQuery.TRANSFORM_OUT))
        .satisfies(
            collection -> {
              TableRow result = collection.iterator().next();
              assertThat(result.get("ticker"), is(equalTo("GOOGL")));
              assertThat(result.get("price"), is(equalTo(1006.94)));
              assertThat(result.get("some_prop"), is(equalTo("someValue")));
              assertThat(result.get("type"), is(equalTo("custom_event")));
              assertThat(result.get("missing"), is(nullValue()));
              assertThat(result.get("message_id"), is(equalTo(topic + " " + messageId)));
              assertThat(result.get("published_at"), is(equalTo(publishTime)));
              return null;
            });

    // Execute pipeline
    pipeline.run();
  }
}
