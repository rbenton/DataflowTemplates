# Workflow for pubsub-cdc-to-bigquery development

This fork of DataflowTemplates was created only to modify the v2/pubsub-cdc-to-bigquery workflow so that its UDF could have access to PubsubMessage content beyond just its `data` property (e.g., `insertId`, `publishTime` and attributes) when computing the new `data` object to output for mapping into a BigQuery row.

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
2. If the flex template pushed by step 1 doesn't look like that in `./copy-ps-cdc-to-bq-spec.sh` (or doesn't exist), run `./copy-ps-cdc-to-bq-spec.sh` and then verify the file at `TEMPLATE_IMAGE_SPEC` is correct.