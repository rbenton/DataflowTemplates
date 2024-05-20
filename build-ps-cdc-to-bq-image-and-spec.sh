# from v2/kafka-to-pubsub/README.md
# gcloud dataflow flex-template build ${TEMPLATE_PATH} \
#   --image-gcr-path "${TARGET_GCR_IMAGE}" \
#   --sdk-language "JAVA" \
#   --flex-template-base-image ${BASE_CONTAINER_IMAGE} \
#   --metadata-file "src/main/resources/kafka_to_pubsub_metadata.json" \
#   --jar "target/kafka-to-pubsub-1.0-SNAPSHOT.jar" \
#   --env FLEX_TEMPLATE_JAVA_MAIN_CLASS="com.google.cloud.teleport.v2.templates.KafkaToPubsub"

# see https://cloud.google.com/sdk/gcloud/reference/dataflow/flex-template/build
gcloud dataflow flex-template build ${TEMPLATE_IMAGE_SPEC} \
  --image-gcr-path=${TARGET_GAR_IMAGE} \
  --jar=v2/pubsub-cdc-to-bigquery/target/pubsub-cdc-to-bigquery-1.0-SNAPSHOT.jar \
  --env=FLEX_TEMPLATE_JAVA_MAIN_CLASS=com.google.cloud.teleport.v2.templates.PubSubCdcToBigQuery \
  --flex-template-base-image=JAVA11 \
  --metadata-file=v2/pubsub-cdc-to-bigquery/src/main/resources/pubsub-cdc-to-bigquery-command-spec.json \
  --sdk-language=JAVA
