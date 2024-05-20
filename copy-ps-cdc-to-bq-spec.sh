# from v2/pubsub-cdc-to-bigquery/README.md, adapted for GAR instead of GCR
echo '{
    "image":"'${TARGET_GAR_IMAGE}'",
    "metadata":{"name":"PubSub CDC to BigQuery",
    "description":"Replicate Pub/Sub Data into BigQuery Tables",
    "parameters":[
        {
            "name":"inputSubscription",
            "label":"PubSub Subscription Name",
            "helpText":"Full subscription reference",
            "paramType":"TEXT"
        },
        {
            "name":"autoMapTables",
            "label":"Automatically add new BigQuery tables and columns as they appear",
            "helpText":"Automatically add new BigQuery tables and columns as they appear",
            "paramType":"TEXT",
            "isOptional":true
        },
        {
            "name":"outputDatasetTemplate",
            "label":"The BigQuery Dataset Name or column template",
            "helpText":"The BigQuery Dataset Name or column template",
            "paramType":"TEXT",
            "isOptional":true
        },
        {
            "name":"outputTableNameTemplate",
            "label":"The BigQuery Table Name or column template",
            "helpText":"The BigQuery Table Name or column template",
            "paramType":"TEXT",
            "isOptional":true
        },
        {
            "name":"outputTableSpec",
            "label":"DEPRECATED: Use outputDatasetTemplate AND outputTableNameTemplate",
            "helpText":"DEPRECATED: Use outputDatasetTemplate AND outputTableNameTemplate",
            "paramType":"TEXT",
            "isOptional":true
        },
        {
            "name":"outputDeadletterTable",
            "label":"Deadletter Queue Table",
            "helpText":"DLQ Table Ref: PROJECT:dataset.dlq",
            "paramType":"TEXT"
        },
        {
            "name":"autoscalingAlgorithm","label":"Autoscaling algorithm to use",
            "helpText":"Autoscaling algorithm to use: THROUGHPUT_BASED",
            "paramType":"TEXT",
            "isOptional":true
        },
        {
            "name":"numWorkers","label":"Number of workers Dataflow will start with",
            "helpText":"Number of workers Dataflow will start with",
            "paramType":"TEXT",
            "isOptional":true
        },
        {
            "name":"maxNumWorkers","label":"Maximum number of workers Dataflow job will use",
            "helpText":"Maximum number of workers Dataflow job will use",
            "paramType":"TEXT",
            "isOptional":true
        },
        {
            "name":"workerMachineType","label":"Worker Machine Type to use in Dataflow Job",
            "helpText":"Machine Type to Use: n1-standard-4",
            "paramType":"TEXT",
            "isOptional":true
        },
        {
            "name":"javascriptTextTransformGcsPath","label":"JavaScript File Path",
            "helpText":"JS File Path",
            "paramType":"TEXT",
            "isOptional":true
        },
        {
            "name":"javascriptTextTransformFunctionName","label":"UDF JavaScript Function Name",
            "helpText":"JS Function Name",
            "paramType":"TEXT",
            "isOptional":true
        },
        {
            "name":"pythonTextTransformGcsPath","label":"Python File Path",
            "helpText":"PY File Path",
            "paramType":"TEXT",
            "isOptional":true
        },
        {
            "name":"pythonTextTransformFunctionName","label":"UDF Python Function Name",
            "helpText":"PY Function Name",
            "paramType":"TEXT",
            "isOptional":true
        }
    ]},
    "sdk_info":{"language":"JAVA"}
}' > image_spec.json
gsutil cp image_spec.json ${TEMPLATE_IMAGE_SPEC}
rm image_spec.json
