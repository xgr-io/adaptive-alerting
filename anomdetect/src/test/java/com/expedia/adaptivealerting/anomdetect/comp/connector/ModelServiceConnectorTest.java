/*
 * Copyright 2018-2019 Expedia Group, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.expedia.adaptivealerting.anomdetect.comp.connector;

import com.expedia.adaptivealerting.anomdetect.DetectorDeserializationException;
import com.expedia.adaptivealerting.anomdetect.DetectorMappingDeserializationException;
import com.expedia.adaptivealerting.anomdetect.DetectorMappingRetrievalException;
import com.expedia.adaptivealerting.anomdetect.DetectorNotFoundException;
import com.expedia.adaptivealerting.anomdetect.DetectorRetrievalException;
import com.expedia.adaptivealerting.anomdetect.detectormapper.Detector;
import com.expedia.adaptivealerting.anomdetect.detectormapper.DetectorMatchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.client.fluent.Content;
import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.expedia.adaptivealerting.anomdetect.comp.connector.ModelServiceConnector.API_PATH_DETECTOR_MAPPING_UPDATES;
import static com.expedia.adaptivealerting.anomdetect.comp.connector.ModelServiceConnector.API_PATH_DETECTOR_UPDATES;
import static com.expedia.adaptivealerting.anomdetect.comp.connector.ModelServiceConnector.API_PATH_MATCHING_DETECTOR_BY_TAGS;
import static com.expedia.adaptivealerting.anomdetect.comp.connector.ModelServiceConnector.API_PATH_MODEL_BY_DETECTOR_UUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * {@link ModelServiceConnector} unit tests.
 */
@Slf4j
public class ModelServiceConnectorTest {
    private static final UUID DETECTOR_UUID = UUID.randomUUID();
    private static final UUID DETECTOR_UUID_CANT_RETRIEVE = UUID.randomUUID();
    private static final UUID DETECTOR_UUID_CANT_DESERIALIZE = UUID.randomUUID();
    private static final UUID DETECTOR_UUID_NO_MODELS = UUID.randomUUID();

    // FIXME The ModelServiceConnector uses this inconsistently. See the notes in that class for more information. [WLW]
    private static final String URI_TEMPLATE = "http://example.com";

    private ModelServiceConnector connectorUnderTest;

    @Mock
    private HttpClientWrapper httpClient;

    @Mock
    private ObjectMapper objectMapper;

    // Test objects - find last updated detectors
    private int invalidTimePeriod = 0;
    private int timePeriod_cantRetrieve = 2;
    private int timePeriod_cantDeserialize = 3;

    // Test object - find Matching Detectors
    private DetectorMatchResponse detectorMatchResponse;
    private Content detectorMatchResponseContent;
    @Mock
    private Content detectorMatchResponseContent_cantDeserialize;

    private List<Map<String, String>> tags = new ArrayList<>();
    private List<Map<String, String>> tags_cantRetrieve = new ArrayList<>();
    private List<Map<String, String>> tags_cantDeserialize = new ArrayList<>();

    private List<DetectorResource> detectorResourceList;

    @Mock
    private Content detectorMappingContent_cantDeserialize;
    private byte[] detectorMappingBytes_cantDeserialize;

    // Test objects - find latest model
    @Mock
    private Content detectorResourcesContent_cantDeserialize;
    private Content detectorResourcesContent;
    private byte[] detectorResourcesBytes_cantDeserialize;
    private DetectorResources detectorResources;
    private DetectorResource detectorResource;
    private Content detectorResourcesContent_noModels;
    private DetectorResource detectorResources_noModel;
    private DetectorResources detectorResources_noModels;

    @Mock
    private Content modelResourcesContent_cantDeserialize;
    private byte[] modelResourcesBytes_cantDeserialize;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        initTestObjects();
        initDependencies();
        this.connectorUnderTest = new ModelServiceConnector(httpClient, URI_TEMPLATE, objectMapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullHttpClient() {
        new ModelServiceConnector(null, URI_TEMPLATE, objectMapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullUriTemplate() {
        new ModelServiceConnector(httpClient, null, objectMapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullObjectMapper() {
        new ModelServiceConnector(httpClient, URI_TEMPLATE, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindUpdatedDetectors_timePeriodInvalid() {
        connectorUnderTest.findUpdatedDetectors(invalidTimePeriod);
    }

    @Test(expected = DetectorRetrievalException.class)
    public void testFindUpdatedDetector_cantRetrieve() {
        connectorUnderTest.findUpdatedDetectors(timePeriod_cantRetrieve);
    }

    @Test(expected = DetectorDeserializationException.class)
    public void testFindUpdatedDetector_cantDeserialize() {
        connectorUnderTest.findUpdatedDetectors(timePeriod_cantDeserialize);
    }

    @Test(expected = DetectorMappingRetrievalException.class)
    public void testFindUpdatedDetectorMappings_cantRetrieve() {
        val result = connectorUnderTest.findUpdatedDetectorMappings(timePeriod_cantRetrieve);
        assertEquals(this.detectorMatchResponse, result);
    }

    @Test(expected = DetectorMappingDeserializationException.class)
    public void testFindUpdatedDetectorMappings_cantDeserialize() {
        connectorUnderTest.findUpdatedDetectorMappings(timePeriod_cantDeserialize);
    }

    @Test
    public void testFindMatchingDetectorMappings() {
        val result = connectorUnderTest.findMatchingDetectorMappings(tags);
        assertEquals(this.detectorMatchResponse, result);
    }

    @Test(expected = DetectorMappingRetrievalException.class)
    public void testFindMatchingDetectorMappings_cantRetrieve() {
        connectorUnderTest.findMatchingDetectorMappings(tags_cantRetrieve);
    }

    @Test(expected = DetectorMappingDeserializationException.class)
    public void testFindMatchingDetectorMappings_cantDeserialize() {
        connectorUnderTest.findMatchingDetectorMappings(tags_cantDeserialize);
    }

    @Test
    public void testFindLatestModel() {
        val result = connectorUnderTest.findLatestModel(DETECTOR_UUID);
        log.info("result:{}", result);
        assertNotNull(result);
    }

    @Test(expected = DetectorRetrievalException.class)
    public void testFindLatestModel_retrievalException() {
        connectorUnderTest.findLatestModel(DETECTOR_UUID_CANT_RETRIEVE);
    }

    @Test(expected = DetectorDeserializationException.class)
    public void testFindLatestModel_deserializationException() {
        connectorUnderTest.findLatestModel(DETECTOR_UUID_CANT_DESERIALIZE);
    }

    @Test(expected = DetectorNotFoundException.class)
    public void testFindLatestModel_notFound() {
        connectorUnderTest.findLatestModel(DETECTOR_UUID_NO_MODELS);
    }

    private void initTestObjects() throws IOException {
        initTestObjects_findDetectors();
        initTestObjects_findMatchingDetectorMappings();
        initTestObjects_findLatestModel();
    }

    private void initTestObjects_findMatchingDetectorMappings() throws IOException {
        // Metrics
        val tag = new HashMap<String, String>();
        tag.put("org_id", "1");
        tag.put("mtype", "count");
        tag.put("unit", "");
        tag.put("what", "bookings");
        tag.put("interval", "5");

        this.tags.add(tag);
        this.tags_cantRetrieve.add(ImmutableMap.of(
                "random", "one",
                "random3", "two"
        ));
        this.tags_cantDeserialize.add(ImmutableMap.of(
                "deserialise", "false"
        ));


        // Find matching detectors - happy path
        ArrayList<Detector> detectors = new ArrayList<>();
        detectors.add(new Detector(UUID.fromString("fe1a2366-a73e-4c9d-9186-474e60df6de8")));
        detectors.add(new Detector(UUID.fromString("65eea7d8-8ec3-4f8a-ab2c-7a9dc873723d")));

        Map<Integer, List<Detector>> groupedDetectorsBySearchIndex = new HashMap<>();
        groupedDetectorsBySearchIndex.put(0, detectors);

        this.detectorMatchResponse = new DetectorMatchResponse(groupedDetectorsBySearchIndex, 4);
        val detectorMatchResponseBytes = new ObjectMapper().writeValueAsBytes(detectorMatchResponse);
        this.detectorMatchResponseContent = new Content(detectorMatchResponseBytes, ContentType.APPLICATION_JSON);


        // Find detectors - cant deserialize
        when(detectorMatchResponseContent_cantDeserialize.asBytes()).thenReturn(detectorResourcesBytes_cantDeserialize);

        this.detectorMappingBytes_cantDeserialize = "detectorMappingBytes_cantDeserialize".getBytes();
        when(detectorMappingContent_cantDeserialize.asBytes()).thenReturn(detectorMappingBytes_cantDeserialize);
    }

    private void initDependencies() throws IOException {
        initDependencies_findDetectors_objectMapper();
        initDependencies_findLatestModel_httpClient();
        initDependencies_findLatestModel_objectMapper();
        initDependencies_findUpdatedDetectors_httpClient();
        initDependencies_findUpdatedDetectorMappings_httpClient();
        initDependencies_findMatchingDetectorMappings_httpClient();
        initDependencies_findMatchingDetectorMappings_objectMapper();
    }

    private void initTestObjects_findDetectors() throws IOException {
        // Find detectors - happy path
        this.detectorResourceList = new ArrayList<>();
        detectorResourceList.add(new DetectorResource("3217d4be-9c33-490f-828e-c976b393b000", "kashah", "constant-detector", new Date(), new HashMap<>(), true));
        detectorResourceList.add(new DetectorResource("90c37a3c-f6bb-4c00-b41b-191909cccfb7", "kashah", "ewma-detector", new Date(), new HashMap<>(), true));

        this.detectorResources = new DetectorResources(detectorResourceList);

        val detectorResourcesBytes = new ObjectMapper().writeValueAsBytes(detectorResources);
        this.detectorResourcesContent = new Content(detectorResourcesBytes, ContentType.APPLICATION_JSON);

        // Find detectors - cant deserialize
        detectorResourcesBytes_cantDeserialize = "detectorResourcesBytes_cantDeserialize".getBytes();
        when(detectorResourcesContent_cantDeserialize.asBytes()).thenReturn(detectorResourcesBytes_cantDeserialize);
    }

    private void initTestObjects_findLatestModel() throws IOException {

        // Find latest model - happy path
        this.detectorResource = new DetectorResource();
        this.detectorResources = new DetectorResources(Collections.singletonList(new DetectorResource("3217d4be-9c33-490f-828e-c976b393b000", "kashah", "constant-detector", new Date(), new HashMap<>(), true)));
        val detectorResourcesBytes = new ObjectMapper().writeValueAsBytes(detectorResources);
        this.detectorResourcesContent = new Content(detectorResourcesBytes, ContentType.APPLICATION_JSON);

        // Find latest model - can't deserialize
        modelResourcesBytes_cantDeserialize = "modelResourcesBytes.cantDeserialize".getBytes();
        when(modelResourcesContent_cantDeserialize.asBytes()).thenReturn(modelResourcesBytes_cantDeserialize);

        // Find latest model - no models
        this.detectorResources_noModels = new DetectorResources(Collections.EMPTY_LIST);
        this.detectorResources_noModel = null;
        val detectorResourcesBytes_noModels = new ObjectMapper().writeValueAsBytes(detectorResources_noModels);
        this.detectorResourcesContent_noModels = new Content(detectorResourcesBytes_noModels, ContentType.APPLICATION_JSON);
    }

    private void initDependencies_findMatchingDetectorMappings_httpClient() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        val reqBody = mapper.writeValueAsString(tags);
        val reqBody_cantRetrieve = mapper.writeValueAsString(tags_cantRetrieve);
        val reqBody_cantDeserialize = mapper.writeValueAsString(tags_cantDeserialize);

        val uri = URI_TEMPLATE + API_PATH_MATCHING_DETECTOR_BY_TAGS;

        when(httpClient.post(uri, reqBody)).thenReturn(detectorMatchResponseContent);
        when(httpClient.post(uri, reqBody_cantRetrieve)).thenThrow(new IOException());
        when(httpClient.post(uri, reqBody_cantDeserialize)).thenReturn(detectorMatchResponseContent_cantDeserialize);

        when(objectMapper.writeValueAsString(tags)).thenReturn(reqBody);
        when(objectMapper.writeValueAsString(tags_cantRetrieve)).thenReturn(reqBody_cantRetrieve);
        when(objectMapper.writeValueAsString(tags_cantDeserialize)).thenReturn(reqBody_cantDeserialize);
    }

    private void initDependencies_findMatchingDetectorMappings_objectMapper() throws IOException {

        when(objectMapper.readValue(detectorMatchResponseContent.asBytes(), DetectorMatchResponse.class))
                .thenReturn(detectorMatchResponse);
        when(objectMapper.readValue(detectorResourcesBytes_cantDeserialize, DetectorMatchResponse.class))
                .thenThrow(new IOException());
    }

    private void initDependencies_findUpdatedDetectors_httpClient() throws IOException {
        val uri_cantRetrieve = String.format(URI_TEMPLATE + API_PATH_DETECTOR_UPDATES, timePeriod_cantRetrieve);
        val uri_cantDeserialize = String.format(URI_TEMPLATE + API_PATH_DETECTOR_UPDATES, timePeriod_cantDeserialize);

        when(httpClient.get(uri_cantRetrieve)).thenThrow(new IOException());
        when(httpClient.get(uri_cantDeserialize)).thenReturn(detectorResourcesContent_cantDeserialize);
    }

    private void initDependencies_findUpdatedDetectorMappings_httpClient() throws IOException {
        val uri_cantRetrieve = String.format(URI_TEMPLATE + API_PATH_DETECTOR_MAPPING_UPDATES, timePeriod_cantRetrieve);
        val uri_cantDeserialize = String.format(URI_TEMPLATE + API_PATH_DETECTOR_MAPPING_UPDATES, timePeriod_cantDeserialize);

        when(httpClient.get(uri_cantRetrieve)).thenThrow(new IOException());
        when(httpClient.get(uri_cantDeserialize)).thenReturn(detectorMappingContent_cantDeserialize);
    }

    private void initDependencies_findDetectors_objectMapper() throws IOException {
        when(objectMapper.readValue(detectorResourcesContent.asBytes(), DetectorResources.class))
                .thenReturn(detectorResources);
        when(objectMapper.readValue(detectorResourcesBytes_cantDeserialize, DetectorResources.class))
                .thenThrow(new IOException());
    }

    private void initDependencies_findLatestModel_httpClient() throws IOException {
        val uri = String.format(URI_TEMPLATE + API_PATH_MODEL_BY_DETECTOR_UUID, DETECTOR_UUID);
        val uri_cantRetrieve = String.format(URI_TEMPLATE + API_PATH_MODEL_BY_DETECTOR_UUID, DETECTOR_UUID_CANT_RETRIEVE);
        val uri_cantDeserialize = String.format(URI_TEMPLATE + API_PATH_MODEL_BY_DETECTOR_UUID, DETECTOR_UUID_CANT_DESERIALIZE);
        val uri_noModels = String.format(URI_TEMPLATE + API_PATH_MODEL_BY_DETECTOR_UUID, DETECTOR_UUID_NO_MODELS);

        when(httpClient.get(uri)).thenReturn(detectorResourcesContent);
        when(httpClient.get(uri_cantRetrieve)).thenThrow(new IOException());
        when(httpClient.get(uri_cantDeserialize)).thenReturn(modelResourcesContent_cantDeserialize);
        when(httpClient.get(uri_noModels)).thenReturn(detectorResourcesContent_noModels);
    }

    private void initDependencies_findLatestModel_objectMapper() throws IOException {
        when(objectMapper.readValue(detectorResourcesContent.asBytes(), DetectorResource.class))
                .thenReturn(detectorResource);
        when(objectMapper.readValue(modelResourcesBytes_cantDeserialize, DetectorResource.class))
                .thenThrow(new IOException());
        when(objectMapper.readValue(detectorResourcesContent_noModels.asBytes(), DetectorResource.class))
                .thenReturn(detectorResources_noModel);
    }
}