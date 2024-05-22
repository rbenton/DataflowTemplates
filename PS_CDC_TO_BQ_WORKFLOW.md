# Workflow for pubsub-cdc-to-bigquery development

This branch of this fork of DataflowTemplates was created only to modify the v2/pubsub-cdc-to-bigquery workflow so that its UDF could have access to PubsubMessage content beyond just its `data` property (e.g., `insertId`, `publishTime` and attributes) when computing the new `data` object to output for mapping into a BigQuery row.

This document is just a record of the steps needed to develop, package and deploy this fork's `pubsub-cdc-to-bigquery` template for use in GCP.

All steps below should be carried out in this project's root folder.

## Setup

The following should be done before any other development, packaging or deployment steps.

1. Install [asdf](https://asdf-vm.com/) and its [java](https://github.com/halcyon/asdf-java) and [maven](https://github.com/halcyon/asdf-maven) plugins to take advantage of this project's `.tool-versions` config.
2. Copy `.envrc.template` to `.envrc` and fill in placeholder values.
3. Run `source .envrc` (or use [direnv](https://github.com/direnv/direnv)) to get these env vars into the shell.
4. Run `gcloud config set project ${PROJECT}`.

## Development

1. Make changes as needed to any Java source and test files.
2. Run `./test-ps-cdc-to-bq.sh` to verify compilation and passing tests.

## Packaging

1. Run `./package-ps-cdc-to-bq.sh` to build the JAR file needed by the deployment steps.

## Deployment

1. Run `./build-ps-cdc-to-bq-image-and-spec.sh` to build and push the template image to the `TARGET_GAR_IMAGE`, and flex template to the `TEMPLATE_IMAGE_SPEC` locations defined in `.envrc`. See https://cloud.google.com/sdk/gcloud/reference/dataflow/flex-template/build and https://cloud.google.com/dataflow/docs/guides/templates/using-flex-templates#build-template for more details.

## Updating

This is just a fork of [GoogleCloudPlatform/DataflowTemplates](https://github.com/GoogleCloudPlatform/DataflowTemplates), so if and when updates from that project are needed here, they can be pulled into this fork's `main` branch, merged into the `ps-cdc-to-bq` branch, and any breaking changes reconciled.

Of particular note is that, per this project's README,

> Templates are released in a weekly basis (best-effort) as part of the efforts to keep [Google-provided Templates](https://cloud.google.com/dataflow/docs/guides/templates/provided-templates) updated with latest fixes and improvements.

and that in order to keep the [pubsub-cdc-to-biqquery-metadata.json](v2%2Fpubsub-cdc-to-bigquery%2Fsrc%2Fmain%2Fresources%2Fpubsub-cdc-to-biqquery-metadata.json) file up-to-date here, per https://cloud.google.com/dataflow/docs/guides/templates/configuring-flex-templates#metadata,

> You can download metadata files for the Google-provided templates from the Dataflow [template directory](https://console.cloud.google.com/storage/browser/dataflow-templates/latest).
