# Workflow for PubSub_to_BigQuery_Flex development

This branch of this fork of DataflowTemplates was created only to modify the v2/googlecloud-to-googlecloud/../PubSub_to_BigQuery_Flex workflow so that its UDF could have access to PubsubMessage content beyond just its `data` property (e.g., `insertId`, `publishTime` and attributes) when computing the new `data` object to output for mapping into a BigQuery row.

This document is just a record of the steps needed to develop, package and deploy this fork's `PubSub_to_BigQuery_Flex` template for use in GCP.

All steps below should be carried out in this project's root folder.

## Setup

The following should be done before any other development, packaging or deployment steps.

1. Install gcloud CLI. See https://cloud.google.com/sdk/docs/install-sdk#installing_the_latest_version.
2. Install [asdf](https://asdf-vm.com/) and its [java](https://github.com/halcyon/asdf-java) and [maven](https://github.com/halcyon/asdf-maven) plugins to take advantage of this project's `.tool-versions` config. On Apple Silicon macs, per [instructions](https://github.com/halcyon/asdf-java?tab=readme-ov-file#apple-silicon-integration) in the asdf-java README, in order to prevent ARM architecture-related compile failures, be sure to `asdf install` java and maven versions in a shell opened with `arch -x86_64 bin/zsh`.
3. Copy `.envrc.template` to `.envrc` and fill in placeholder values.
4. Run `source .envrc` (or use [direnv](https://github.com/direnv/direnv)) to get these env vars into the shell.
5. Run `gcloud config set project ${PROJECT}`.
6. Run `mvn clean install -pl plugins/templates-maven-plugin -am`.

## Development

1. Make changes as needed to any Java source and test files.
2. Run `./test-ps-to-bq-flex.sh` to verify compilation and passing tests.

## Packaging

1. Run `./package-gc-to-gc.sh` to build the JAR file and metadata JSON needed by the deployment steps.

## Deployment

1. Run `./stage-ps-to-bq-flex.sh` to build and push the template image to the GAR, and flex template to the GS bucket, locations defined in `.envrc`. See https://cloud.google.com/sdk/gcloud/reference/dataflow/flex-template/build and https://cloud.google.com/dataflow/docs/guides/templates/using-flex-templates#build-template for more details.

## Updating

This is just a fork of [GoogleCloudPlatform/DataflowTemplates](https://github.com/GoogleCloudPlatform/DataflowTemplates), so if and when updates from that project are needed here, they can be pulled into this fork's `main` branch, merged into the `ps-to-bq` branch, and any breaking changes reconciled.

Of particular note is that, per this project's README,

> Templates are released in a weekly basis (best-effort) as part of the efforts to keep [Google-provided Templates](https://cloud.google.com/dataflow/docs/guides/templates/provided-templates) updated with latest fixes and improvements.
