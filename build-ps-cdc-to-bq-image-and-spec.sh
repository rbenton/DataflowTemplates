# see https://cloud.google.com/sdk/gcloud/reference/dataflow/flex-template/build
# metadata file extracted from https://console.cloud.google.com/storage/browser/_details/dataflow-templates/latest/flex/PubSub_CDC_to_BigQuery;tab=live_object
gcloud dataflow flex-template build "${TEMPLATE_IMAGE_SPEC}" \
  --image-gcr-path="${TARGET_GAR_IMAGE}" \
  --jar="v2/pubsub-cdc-to-bigquery/target/pubsub-cdc-to-bigquery-1.0-SNAPSHOT.jar" \
  --env="FLEX_TEMPLATE_JAVA_MAIN_CLASS=com.google.cloud.teleport.v2.templates.PubSubCdcToBigQuery" \
  --flex-template-base-image="JAVA11" \
  --metadata-file="v2/pubsub-cdc-to-bigquery/src/main/resources/pubsub-cdc-to-bigquery-metadata.json" \
  --sdk-language="JAVA"