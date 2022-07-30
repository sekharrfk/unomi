/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package org.apache.unomi.itests.migration;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.unomi.api.*;
import org.apache.unomi.itests.BaseIT;
import org.apache.unomi.persistence.spi.aggregate.TermsAggregate;
import org.apache.unomi.shell.migration.utils.HttpUtils;
import org.apache.unomi.shell.migration.utils.MigrationUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Migrate16xTo200IT extends BaseIT {

    @Override
    @Before
    public void waitForStartup() throws InterruptedException {

        // Restore snapshot from 1.6.x
        try (CloseableHttpClient httpClient = HttpUtils.initHttpClient(true, null)) {
            // Create snapshot repo
            HttpUtils.executePutRequest(httpClient, "http://localhost:9400/_snapshot/snapshots_repository/", resourceAsString("migration/create_snapshots_repository.json"), null);
            // Get snapshot, insure it exists
            String snapshot = HttpUtils.executeGetRequest(httpClient, "http://localhost:9400/_snapshot/snapshots_repository/snapshot_1.6.x", null);
            if (snapshot == null || !snapshot.contains("snapshot_1.6.x")) {
                throw new RuntimeException("Unable to retrieve 1.6.x snapshot for ES restore");
            }
            // Restore the snapshot
            HttpUtils.executePostRequest(httpClient, "http://localhost:9400/_snapshot/snapshots_repository/snapshot_1.6.x/_restore?wait_for_completion=true", "{}",  null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Do migrate the data set
        executeCommand("unomi:migrate 1.6.0 true");
        // Call super for starting Unomi and wait for the complete startup
        super.waitForStartup();
    }

    @After
    public void cleanup() throws InterruptedException {
        removeItems(Profile.class);
        removeItems(Session.class);
        removeItems(Event.class);
        removeItems(Scope.class);
    }

    @Test
    public void checkMigratedData() throws Exception {
        checkProfileInterests();
        checkScopeHaveBeenCreated();
        checkFormEventRestructured();
        checkViewEventRestructured();
        checkEventTypesNotPersistedAnymore();
        checkForMappingUpdates();
    }

    /**
     * Multiple index mappings have been update, check a simple check that after migration those mappings contains the latest modifications.
     */
    private void checkForMappingUpdates() throws IOException {
        Assert.assertTrue(HttpUtils.executeGetRequest(httpClient, "http://localhost:9400/context-scope/_mapping", null).contains("\"match\":\"*\",\"match_mapping_type\":\"string\",\"mapping\":{\"analyzer\":\"folding\""));
        Assert.assertTrue(HttpUtils.executeGetRequest(httpClient, "http://localhost:9400/context-segment/_mapping", null).contains("\"condition\":{\"type\":\"object\",\"enabled\":false}"));
        Assert.assertTrue(HttpUtils.executeGetRequest(httpClient, "http://localhost:9400/context-scoring/_mapping", null).contains("\"condition\":{\"type\":\"object\",\"enabled\":false}"));
        Assert.assertTrue(HttpUtils.executeGetRequest(httpClient, "http://localhost:9400/context-campaign/_mapping", null).contains("\"entryCondition\":{\"type\":\"object\",\"enabled\":false}"));
        Assert.assertTrue(HttpUtils.executeGetRequest(httpClient, "http://localhost:9400/context-conditiontype/_mapping", null).contains("\"parentCondition\":{\"type\":\"object\",\"enabled\":false}"));
        Assert.assertTrue(HttpUtils.executeGetRequest(httpClient, "http://localhost:9400/context-goal/_mapping", null).contains("\"startEvent\":{\"type\":\"object\",\"enabled\":false}"));
        Assert.assertTrue(HttpUtils.executeGetRequest(httpClient, "http://localhost:9400/context-patch/_mapping", null).contains("\"data\":{\"type\":\"object\",\"enabled\":false}"));
        Assert.assertTrue(HttpUtils.executeGetRequest(httpClient, "http://localhost:9400/context-rule/_mapping", null).contains("\"condition\":{\"type\":\"object\",\"enabled\":false}"));
        Assert.assertTrue(HttpUtils.executeGetRequest(httpClient, "http://localhost:9400/context-profile/_mapping", null).contains("\"interests\":{\"type\":\"nested\""));
        for (String eventIndex : MigrationUtils.getIndexesPrefixedBy(httpClient, "http://localhost:9400", "context-event-")) {
            Assert.assertTrue(HttpUtils.executeGetRequest(httpClient, "http://localhost:9400/" + eventIndex + "/_mapping", null).contains("\"flattenedProperties\":{\"type\":\"flattened\"}"));
        }
    }

    /**
     * Data set contains a form event (id: 7b55b4fd-5ff0-4a85-9dc4-ffde322a1de6) with this data:
     * {
     *   "properties": {
     *     "pets": "cat",
     *     "firstname": "foo",
     *     "sports": [
     *       "football",
     *       "tennis"
     *     ],
     *     "city": "Berlin",
     *     "age": "15",
     *     "email": "foo@bar.fr",
     *     "drone": "dewey",
     *     "lastname": "bar",
     *     "contactMethod": [
     *       "postalMethod",
     *       "phoneMethod"
     *     ]
     *   }
     * }
     */
    private void checkFormEventRestructured() {
        List<Event> events = persistenceService.query("eventType", "form", null, Event.class);
        for (Event formEvent : events) {
            Assert.assertEquals(0, formEvent.getProperties().size());
            Map<String, Object> fields = (Map<String, Object>) formEvent.getFlattenedProperties().get("fields");
            Assert.assertTrue(fields.size() > 0);

            if (Objects.equals(formEvent.getItemId(), "7b55b4fd-5ff0-4a85-9dc4-ffde322a1de6")) {
                // check singled valued
                Assert.assertEquals("cat", fields.get("pets"));
                // check multi-valued
                List<String> sports = (List<String>) fields.get("sports");
                Assert.assertEquals(2, sports.size());
                Assert.assertTrue(sports.contains("football"));
                Assert.assertTrue(sports.contains("tennis"));
            }
        }
    }

    /**
     * Data set contains a view event (id: a4aa836b-c437-48ef-be02-6fbbcba3a1de) with two interests: football:50 and basketball:30
     * Data set contains a view event (id: 34d53399-f173-451f-8d48-f34f5d9618a9) with two URL Parameters: paramerter_test:value, multiple_paramerter_test:[value1, value2]
     */
    private void checkViewEventRestructured() {
        List<Event> events = persistenceService.query("eventType", "view", null, Event.class);
        for (Event viewEvent : events) {

            // check interests
            if (Objects.equals(viewEvent.getItemId(), "a4aa836b-c437-48ef-be02-6fbbcba3a1de")) {
                CustomItem target = (CustomItem) viewEvent.getTarget();
                Assert.assertNull(target.getProperties().get("interests"));
                Map<String, Object> interests = (Map<String, Object>) viewEvent.getFlattenedProperties().get("interests");
                Assert.assertEquals(30, interests.get("basketball"));
                Assert.assertEquals(50, interests.get("football"));
            }

            // check URL parameters
            if (Objects.equals(viewEvent.getItemId(), "34d53399-f173-451f-8d48-f34f5d9618a9")) {
                CustomItem target = (CustomItem) viewEvent.getTarget();
                Map<String, Object> pageInfo = (Map<String, Object>) target.getProperties().get("pageInfo");
                Assert.assertNull(pageInfo.get("parameters"));
                Map<String, Object> parameters = (Map<String, Object>) viewEvent.getFlattenedProperties().get("URLParameters");
                Assert.assertEquals("value", parameters.get("paramerter_test"));
                List<String> multipleParameterTest = (List<String>) parameters.get("multiple_paramerter_test");
                Assert.assertEquals(2, multipleParameterTest.size());
                Assert.assertTrue(multipleParameterTest.contains("value1"));
                Assert.assertTrue(multipleParameterTest.contains("value2"));
            }
        }
    }


    /**
     * Data set contains 2 events that are not persisted anymore:
     * One updateProperties event
     * One sessionCreated event
     * This test ensures that both have been removed.
     */
    private void checkEventTypesNotPersistedAnymore() {
        Assert.assertEquals(0, persistenceService.query("eventType", "updateProperties", null, Event.class).size());
        Assert.assertEquals(0, persistenceService.query("eventType", "sessionCreated", null, Event.class).size());
    }

    /**
     * Data set contains multiple events, this test is generic enough to ensure all existing events have the scope created correctly
     * So the data set can contain multiple different scope it's not a problem.
     */
    private void checkScopeHaveBeenCreated() {
        // check that the scope mySite have been created based on the previous existings events
        Map<String, Long> existingScopesFromEvents = persistenceService.aggregateWithOptimizedQuery(null, new TermsAggregate("scope"), Event.ITEM_TYPE);
        for (String scopeFromEvents : existingScopesFromEvents.keySet()) {
            if (!Objects.equals(scopeFromEvents, "_filtered")) {
                Scope scope = scopeService.getScope(scopeFromEvents);
                Assert.assertNotNull(scope);
            }
        }
    }

    /**
     * Data set contains a profile (id: e67ecc69-a7b3-47f1-b91f-5d6e7b90276e) with two interests: football:50 and basketball:30
     * Also it's first name is test_profile
     */
    private void checkProfileInterests() {
        // check that the test_profile interests have been migrated to new data structure
        Profile profile = persistenceService.load("e67ecc69-a7b3-47f1-b91f-5d6e7b90276e", Profile.class);
        Assert.assertEquals("test_profile", profile.getProperty("firstName"));

        List<Map<String, Object>> interests = (List<Map<String, Object>>) profile.getProperty("interests");
        Assert.assertEquals(2, interests.size());
        for (Map<String, Object> interest : interests) {
            if ("basketball".equals(interest.get("key"))) {
                Assert.assertEquals(30, interest.get("value"));
            }
            if ("football".equals(interest.get("key"))) {
                Assert.assertEquals(50, interest.get("value"));
            }
        }
    }
}
