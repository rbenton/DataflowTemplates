/*
 * Copyright (C) 2022 Google LLC
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

import static com.google.cloud.teleport.it.matchers.TemplateAsserts.assertThatPipeline;
import static com.google.cloud.teleport.it.matchers.TemplateAsserts.assertThatRecords;
import static com.google.cloud.teleport.it.matchers.TemplateAsserts.assertThatResult;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Field.Mode;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.teleport.it.TemplateTestBase;
import com.google.cloud.teleport.it.bigquery.BigQueryResourceManager;
import com.google.cloud.teleport.it.bigquery.DefaultBigQueryResourceManager;
import com.google.cloud.teleport.it.bigtable.DefaultBigtableResourceManager;
import com.google.cloud.teleport.it.kafka.DefaultKafkaResourceManager;
import com.google.cloud.teleport.it.launcher.PipelineLauncher.LaunchConfig;
import com.google.cloud.teleport.it.launcher.PipelineLauncher.LaunchInfo;
import com.google.cloud.teleport.it.launcher.PipelineOperator.Result;
import com.google.cloud.teleport.metadata.TemplateIntegrationTest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Integration test for {@link KafkaToBigQuery} (Kafka_To_BigQuery). */
@Category(TemplateIntegrationTest.class)
@TemplateIntegrationTest(KafkaToBigQuery.class)
@RunWith(JUnit4.class)
public final class KafkaToBigQueryIT extends TemplateTestBase {

  @Rule public final TestName testName = new TestName();

  private static final Logger LOG = LoggerFactory.getLogger(DefaultBigtableResourceManager.class);

  private DefaultKafkaResourceManager kafkaResourceManager;
  private BigQueryResourceManager bigQueryClient;

  @Before
  public void setup() throws IOException {
    bigQueryClient =
        DefaultBigQueryResourceManager.builder(testName.getMethodName(), PROJECT)
            .setCredentials(credentials)
            .build();
    bigQueryClient.createDataset(REGION);

    kafkaResourceManager =
        DefaultKafkaResourceManager.builder(testName.getMethodName())
            .setNumTopics(0)
            .setHost(HOST_IP)
            .build();
  }

  @After
  public void tearDownClass() {
    boolean producedError = false;

    try {
      kafkaResourceManager.cleanupAll();
    } catch (Exception e) {
      LOG.error("Failed to delete Kafka resources.", e);
      producedError = true;
    }

    try {
      bigQueryClient.cleanupAll();
    } catch (Exception e) {
      LOG.error("Failed to delete BigQuery resources.", e);
      producedError = true;
    }

    if (producedError) {
      throw new IllegalStateException("Failed to delete resources. Check above for errors.");
    }
  }

  @Test
  public void testKafkaToBigQuery() throws IOException {
    baseKafkaToBigQuery(Function.identity()); // no extra parameters
  }

  @Test
  public void testKafkaToBigQueryWithExistingDLQ() throws IOException {
    TableId deadletterTableId =
        bigQueryClient.createTable(testName.getMethodName() + "_dlq", getDeadletterSchema());

    baseKafkaToBigQuery(
        b -> b.addParameter("outputDeadletterTable", toTableSpec(deadletterTableId)));
  }

  @Test
  public void testKafkaToBigQueryWithStorageApi() throws IOException {
    baseKafkaToBigQuery(
        b ->
            b.addParameter("useStorageWriteApi", "true")
                .addParameter("numStorageWriteApiStreams", "3")
                .addParameter("storageWriteApiTriggeringFrequencySec", "3"));
  }

  @Test
  public void testKafkaToBigQueryWithStorageApiExistingDLQ() throws IOException {
    TableId deadletterTableId =
        bigQueryClient.createTable(testName.getMethodName() + "_dlq", getDeadletterSchema());

    baseKafkaToBigQuery(
        b ->
            b.addParameter("useStorageWriteApi", "true")
                .addParameter("numStorageWriteApiStreams", "3")
                .addParameter("storageWriteApiTriggeringFrequencySec", "3")
                .addParameter("outputDeadletterTable", toTableSpec(deadletterTableId)));
  }

  private Schema getDeadletterSchema() {
    Schema dlqSchema =
        Schema.of(
            Field.newBuilder("timestamp", StandardSQLTypeName.TIMESTAMP)
                .setMode(Mode.REQUIRED)
                .build(),
            Field.newBuilder("payloadString", StandardSQLTypeName.STRING)
                .setMode(Mode.REQUIRED)
                .build(),
            Field.newBuilder("payloadBytes", StandardSQLTypeName.BYTES)
                .setMode(Mode.REQUIRED)
                .build(),
            Field.newBuilder(
                    "attributes",
                    StandardSQLTypeName.STRUCT,
                    Field.newBuilder("key", StandardSQLTypeName.STRING)
                        .setMode(Mode.NULLABLE)
                        .build(),
                    Field.newBuilder("value", StandardSQLTypeName.STRING)
                        .setMode(Mode.NULLABLE)
                        .build())
                .setMode(Mode.REPEATED)
                .build(),
            Field.newBuilder("errorMessage", StandardSQLTypeName.STRING)
                .setMode(Mode.NULLABLE)
                .build(),
            Field.newBuilder("stacktrace", StandardSQLTypeName.STRING)
                .setMode(Mode.NULLABLE)
                .build());
    return dlqSchema;
  }

  public void baseKafkaToBigQuery(Function<LaunchConfig.Builder, LaunchConfig.Builder> paramsAdder)
      throws IOException {
    // Arrange
    String topicName = kafkaResourceManager.createTopic(testName.getMethodName(), 5);

    String bqTable = testName.getMethodName();
    Schema bqSchema =
        Schema.of(
            Field.of("id", StandardSQLTypeName.INT64),
            Field.of("name", StandardSQLTypeName.STRING));

    TableId tableId = bigQueryClient.createTable(bqTable, bqSchema);
    TableId deadletterTableId = TableId.of(tableId.getDataset(), tableId.getTable() + "_dlq");

    LaunchConfig.Builder options =
        paramsAdder.apply(
            LaunchConfig.builder(testName, specPath)
                .addParameter(
                    "bootstrapServers",
                    kafkaResourceManager.getBootstrapServers().replace("PLAINTEXT://", ""))
                .addParameter("inputTopics", topicName)
                .addParameter("outputTableSpec", toTableSpec(tableId))
                .addParameter("outputDeadletterTable", toTableSpec(deadletterTableId)));

    // Act
    LaunchInfo info = launchTemplate(options);
    assertThatPipeline(info).isRunning();
    KafkaProducer<String, String> kafkaProducer =
        kafkaResourceManager.buildProducer(new StringSerializer(), new StringSerializer());

    for (int i = 1; i <= 10; i++) {
      publish(kafkaProducer, topicName, i + "1", "{\"id\": " + i + "1, \"name\": \"Dataflow\"}");
      publish(kafkaProducer, topicName, i + "2", "{\"id\": " + i + "2, \"name\": \"Pub/Sub\"}");
      // Invalid schema
      publish(
          kafkaProducer, topicName, i + "3", "{\"id\": " + i + "3, \"description\": \"Pub/Sub\"}");

      try {
        TimeUnit.SECONDS.sleep(3);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    Result result =
        pipelineOperator()
            .waitForCondition(
                createConfig(info),
                () ->
                    bigQueryClient.readTable(tableId.getTable()).getTotalRows() >= 20
                        && bigQueryClient.readTable(deadletterTableId.getTable()).getTotalRows()
                            >= 10);

    // Assert
    assertThatResult(result).meetsConditions();

    TableResult tableRows = bigQueryClient.readTable(bqTable);
    assertThatRecords(tableRows)
        .hasRecordsUnordered(
            List.of(Map.of("id", 11, "name", "Dataflow"), Map.of("id", 12, "name", "Pub/Sub")));
  }

  private void publish(
      KafkaProducer<String, String> producer, String topicName, String key, String value) {
    try {
      RecordMetadata recordMetadata =
          producer.send(new ProducerRecord<>(topicName, key, value)).get();
      LOG.info(
          "Published record {}, partition {} - offset: {}",
          recordMetadata.topic(),
          recordMetadata.partition(),
          recordMetadata.offset());
    } catch (Exception e) {
      throw new RuntimeException("Error publishing record to Kafka", e);
    }
  }
}