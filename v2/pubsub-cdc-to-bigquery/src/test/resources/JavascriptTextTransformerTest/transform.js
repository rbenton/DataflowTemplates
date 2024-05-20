/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * A good transform function.
 * @param {string} inJson
 * @return {string} outJson
 */
function transform(inJson) {
  var obj = JSON.parse(inJson);
  // Example data transformations:
  // Add a field: obj.newField = 1;
  // Modify a field: obj.existingField = '';
  // Filter a record: return null;
  // TODO: create one function per topic
  var topic = "projects/project-id/topics/topic-id";
  obj.data.message_id = topic + " " + obj.messageId;
  obj.data.published_at = obj.publishTime;
  obj.data.some_prop = "someValue";
  obj.data.type = obj.attributes ? obj.attributes.type : null;
  obj.data.missing = obj.attributes ? obj.attributes.missing : null;
  return JSON.stringify(obj);
}

/**
 * A transform function which only accepts 42 as the answer to life.
 * @param {string} inJson
 * @return {string} outJson
 */
function transformWithFilter(inJson) {
  var obj = JSON.parse(inJson);
  // only output objects which have an answer to life of 42.
  if (!(obj.hasOwnProperty('answerToLife') && obj.answerToLife != 42)) {
    return JSON.stringify(obj);
  }
}