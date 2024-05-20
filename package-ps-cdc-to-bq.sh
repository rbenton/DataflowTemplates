# see v2/pubsub-cdc-to-bigquery/README.md
mvn clean package \
  -Dimage=${TARGET_GAR_IMAGE} \
  -Dbase-container-image=${BASE_CONTAINER_IMAGE} \
  -Dbase-container-image.version=${BASE_CONTAINER_IMAGE_VERSION} \
  -Dapp-root=${APP_ROOT} \
  -Dcommand-spec=${DATAFLOW_JAVA_COMMAND_SPEC} \
  -am -pl :pubsub-cdc-to-bigquery
