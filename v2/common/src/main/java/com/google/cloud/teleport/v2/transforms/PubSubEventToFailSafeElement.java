/*
 * Copyright (C) 2020 Google LLC
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
package com.google.cloud.teleport.v2.transforms;

import com.google.cloud.teleport.v2.values.FailsafeElement;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.transforms.DoFn;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * The {@link PubSubEventToFailSafeElement} wraps an incoming {@link PubsubMessage} with the {@link
 * FailsafeElement} class so errors can be recovered from and the original message can be output to
 * a error records table.
 */
public class PubSubEventToFailSafeElement
    extends DoFn<PubsubMessage, FailsafeElement<String, String>> {

  /** GSON to process a {@link PubsubMessage}. */
  private static final Gson GSON = new Gson();

  private static final DateTimeFormatter ISO_DATETIME_FORMAT = ISODateTimeFormat.dateTime();

  protected static final String PUBSUB_MESSAGE_ATTRIBUTE_FIELD = "attributes";
  protected static final String PUBSUB_MESSAGE_DATA_FIELD = "data";
  private static final String PUBSUB_MESSAGE_ID_FIELD = "messageId";
  private static final String PUBSUB_PUBLISH_TIME_FIELD = "publishTime";

  @ProcessElement
  public void processElement(ProcessContext context) {
    PubsubMessage message = context.element();
    // see https://lists.apache.org/thread/w5kwshmd6brtbx51kj2603xk8gynx66j
    Instant timestamp = context.timestamp();
    context.output(
        FailsafeElement.of(
            formatPubsubMessage(message, timestamp), formatPubsubMessage(message, timestamp)));
  }

  public static PubSubEventToFailSafeElement create() {
    return new PubSubEventToFailSafeElement();
  }

  /**
   * Utility method that formats {@link org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage} according
   * to the model defined in {@link com.google.pubsub.v1.PubsubMessage}.
   *
   * @param pubsubMessage {@link org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage}
   * @return JSON String that adheres to the model defined in {@link
   *     com.google.pubsub.v1.PubsubMessage}
   */
  protected static String formatPubsubMessage(PubsubMessage pubsubMessage, Instant timestamp) {
    JsonObject messageJson = new JsonObject();

    String payload = new String(pubsubMessage.getPayload(), StandardCharsets.UTF_8);
    try {
      JsonObject data = GSON.fromJson(payload, JsonObject.class);
      messageJson.add(PUBSUB_MESSAGE_DATA_FIELD, data);
    } catch (JsonSyntaxException e) {
      messageJson.addProperty(PUBSUB_MESSAGE_DATA_FIELD, payload);
    }

    JsonObject attributes = getAttributesJson(pubsubMessage.getAttributeMap());
    messageJson.add(PUBSUB_MESSAGE_ATTRIBUTE_FIELD, attributes);

    if (pubsubMessage.getTopic() != null) {
      messageJson.addProperty("topic", pubsubMessage.getTopic());
    }

    if (pubsubMessage.getMessageId() != null) {
      messageJson.addProperty(PUBSUB_MESSAGE_ID_FIELD, pubsubMessage.getMessageId());
    }

    messageJson.addProperty(PUBSUB_PUBLISH_TIME_FIELD, ISO_DATETIME_FORMAT.print(timestamp));

    return messageJson.toString();
  }

  /**
   * Constructs a {@link JsonObject} from a {@link Map} of Pub/Sub attributes.
   *
   * @param attributesMap {@link Map} of Pub/Sub attributes
   * @return {@link JsonObject} of Pub/Sub attributes
   */
  private static JsonObject getAttributesJson(Map<String, String> attributesMap) {
    JsonObject attributesJson = new JsonObject();
    for (String key : attributesMap.keySet()) {
      attributesJson.addProperty(key, attributesMap.get(key));
    }

    return attributesJson;
  }
}
