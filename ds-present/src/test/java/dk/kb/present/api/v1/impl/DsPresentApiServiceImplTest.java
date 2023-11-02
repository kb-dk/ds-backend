package dk.kb.present.api.v1.impl;

import dk.kb.license.client.v1.DsLicenseApi;
import dk.kb.license.invoker.v1.ApiException;
import dk.kb.license.model.v1.CheckAccessForIdsInputDto;
import dk.kb.license.model.v1.CheckAccessForIdsOutputDto;
import dk.kb.present.PresentFacade;
import dk.kb.present.PresentFacadeTest;
import dk.kb.present.config.ServiceConfig;
import dk.kb.present.webservice.exception.ForbiddenServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.reflection.FieldSetter.setField;

/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
class DsPresentApiServiceImplTest {

    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize("test_setup.yaml");
            PresentFacade.warmUp();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    public void testSingleRecordLicense() throws NoSuchFieldException, ApiException {
        final String RECORD_ID = "local.mods:40221e30-1414-11e9-8fb8-00505688346e.xml";

        // Setup mock license that allows RECORD_ID
        DsPresentApiServiceImpl presentAPI = getMockedPresentAPI();
        DsLicenseApi mockedLicenseClient = mock(DsLicenseApi.class);
        CheckAccessForIdsOutputDto accessResponse = new CheckAccessForIdsOutputDto().accessIds(List.of(RECORD_ID));
        doReturn(accessResponse).when(mockedLicenseClient).checkAccessForIds(any(CheckAccessForIdsInputDto.class));
        DsPresentApiServiceImpl.licenseClient = mockedLicenseClient;
        String record = presentAPI.getRecord(RECORD_ID, "mods");
        assertTrue(record.contains("<mets:mets "), "Extraction with accepting license client should work");

        // Change the mock to not allow the record
        CheckAccessForIdsOutputDto noAccessResponse = new CheckAccessForIdsOutputDto().nonAccessIds(List.of(RECORD_ID));
        doReturn(noAccessResponse).when(mockedLicenseClient).checkAccessForIds(any(CheckAccessForIdsInputDto.class));
        assertThrows(ForbiddenServiceException.class, () -> presentAPI.getRecord(RECORD_ID, "mods"),
                "Calling getRecord should not be allowed");

        // Change the mock to not have the record
        CheckAccessForIdsOutputDto noRecordResponse = new CheckAccessForIdsOutputDto().nonExistingIds(List.of(RECORD_ID));
        doReturn(noRecordResponse).when(mockedLicenseClient).checkAccessForIds(any(CheckAccessForIdsInputDto.class));
        assertThrows(NotFoundServiceException.class, () -> presentAPI.getRecord(RECORD_ID, "mods"),
                "Calling getRecord should raise a not found exception");
    }

    @Test
    public void testMultiRecordsLicense() throws NoSuchFieldException, ApiException, IOException {
        // No local.mods-prefix here. This is an oversight of the file based test-storage
        final String RECORD_ID1 = "40221e30-1414-11e9-8fb8-00505688346e.xml";
        final String RECORD_ID2 = "5cc1bea0-71fa-11e2-b31c-0016357f605f.xml";
        final String RECORD_ID3 = "3956d820-7b7d-11e6-b2b3-0016357f605f.xml";

        // Setup mock license that allow only 3 known records.
        DsPresentApiServiceImpl presentAPI = getMockedPresentAPI();
        DsLicenseApi mockedLicenseClient = mock(DsLicenseApi.class);
        CheckAccessForIdsOutputDto accessResponse = new CheckAccessForIdsOutputDto().accessIds(List.of(
                RECORD_ID1, RECORD_ID2, RECORD_ID3));
        doReturn(accessResponse).when(mockedLicenseClient).checkAccessForIds(any(CheckAccessForIdsInputDto.class));
        DsPresentApiServiceImpl.licenseClient = mockedLicenseClient;
        StreamingOutput records = presentAPI.getRecords("dsfl", 0L, 1000L, "mods");
        assertEquals(3, PresentFacadeTest.countMETS(records),
                "3-specific-records-accepting license should return exactly 3 records");

        // Change the mock to not allow any record
        accessResponse = new CheckAccessForIdsOutputDto().accessIds(Collections.emptyList());
        doReturn(accessResponse).when(mockedLicenseClient).checkAccessForIds(any(CheckAccessForIdsInputDto.class));
        StreamingOutput noRecords = presentAPI.getRecords("dsfl", 0L, 1000L, "mods");
        assertEquals(0, PresentFacadeTest.countMETS(noRecords),
                "Zero accepting license should return exactly 0 records");
    }

    /**
     * Basic mocking of the {@link DsPresentApiServiceImpl}. Callers should add further mocking.
     * @return a Mochito mock of {@code } DsPresentApiServiceImpl with {@code httpServletRequest}.
     * @throws NoSuchFieldException
     */
    private static DsPresentApiServiceImpl getMockedPresentAPI() throws NoSuchFieldException {
        HttpServletMapping mockedMapping = mock(HttpServletMapping.class);
        doReturn("foo").when(mockedMapping).getMatchValue();

        HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
        doReturn("GET").when(mockedRequest).getMethod();
        doReturn(mockedMapping).when(mockedRequest).getHttpServletMapping();
        doReturn(Collections.enumeration(List.of("foo/bar"))).when(mockedRequest).getHeaders("Accept");

        DsPresentApiServiceImpl presentAPI = spy(new DsPresentApiServiceImpl());
        setField(presentAPI, dk.kb.util.webservice.ImplBase.class.getDeclaredField("httpServletRequest"), mockedRequest);
        return presentAPI;
    }
}