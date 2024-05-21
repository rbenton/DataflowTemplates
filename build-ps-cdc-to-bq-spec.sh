# see https://cloud.google.com/sdk/gcloud/reference/dataflow/flex-template/build
# metadata file extracted from https://console.cloud.google.com/storage/browser/_details/dataflow-templates/latest/flex/PubSub_CDC_to_BigQuery;tab=live_object
gcloud dataflow flex-template build "${TEMPLATE_IMAGE_SPEC}" \
  --image="${TARGET_GAR_IMAGE}" \
  --metadata-file="v2/pubsub-cdc-to-bigquery/src/main/resources/pubsub-cdc-to-bigquery-metadata.json" \
  --sdk-language="JAVA"
