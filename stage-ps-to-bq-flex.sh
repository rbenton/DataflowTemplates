#mvn clean package -PtemplatesStage  \
#  -DskipTests \
#  -DprojectId="$PROJECT" \
#  -DbucketName="$BUCKET_NAME" \
#  -DstagePrefix="templates" \
#  -DtemplateName="PubSub_to_BigQuery_Flex" \
#  -am -pl v2/googlecloud-to-googlecloud

# see https://cloud.google.com/sdk/gcloud/reference/dataflow/flex-template/build
gcloud dataflow flex-template build "${BUCKET_NAME}/templates/flex/PubSub_to_BigQuery_Flex" \
  --image-gcr-path="${LOCATION}-docker.pkg.dev/${PROJECT}/images/pubsub-to-bigquery:latest" \
  --jar="v2/googlecloud-to-googlecloud/target/googlecloud-to-googlecloud-1.0-SNAPSHOT.jar" \
  --env="FLEX_TEMPLATE_JAVA_MAIN_CLASS=com.google.cloud.teleport.v2.templates.PubSubToBigQuery" \
  --flex-template-base-image="JAVA11" \
  --metadata-file="v2/googlecloud-to-googlecloud/target/pubsub-to-bigquery-generated-metadata.json" \
  --sdk-language="JAVA"
