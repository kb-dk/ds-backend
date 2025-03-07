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
package dk.kb.present.api.v1.impl;

import dk.kb.license.model.v1.CheckAccessForIdsInputDto;
import dk.kb.license.model.v1.CheckAccessForIdsOutputDto;
import dk.kb.license.util.DsLicenseClient;
import dk.kb.present.PresentFacade;
import dk.kb.present.PresentFacadeTest;
import dk.kb.present.config.ServiceConfig;
import dk.kb.present.model.v1.FormatDto;
import dk.kb.present.webservice.AccessUtil;
import dk.kb.present.webservice.exception.ForbiddenServiceException;
import dk.kb.util.Resolver;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import dk.kb.util.webservice.exception.ServiceException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static dk.kb.present.util.ReflectUtils.setField;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DsPresentApiServiceImplTest {

    HttpServletResponse testResponse;

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
    
    public void testSingleRecordLicense() throws NoSuchFieldException, ServiceException {
        final String RECORD_ID = "local.mods:40221e30-1414-11e9-8fb8-00505688346e.xml";

        // Setup mock license that allows RECORD_ID
        DsPresentApiServiceImpl presentAPI = getMockedPresentAPI();
        DsLicenseClient mockedLicenseClient = mock(DsLicenseClient.class);
        CheckAccessForIdsOutputDto accessResponse = new CheckAccessForIdsOutputDto().accessIds(List.of(RECORD_ID));
        doReturn(accessResponse).when(mockedLicenseClient).checkAccessForIds(any(CheckAccessForIdsInputDto.class));
        AccessUtil.licenseClient = mockedLicenseClient;
        String record = presentAPI.getRecord(RECORD_ID, FormatDto.MODS);
        assertTrue(record.contains("<mets:mets "), "Extraction with accepting license client should work");

        // Change the mock to not allow the record
        CheckAccessForIdsOutputDto noAccessResponse = new CheckAccessForIdsOutputDto().nonAccessIds(List.of(RECORD_ID));
        doReturn(noAccessResponse).when(mockedLicenseClient).checkAccessForIds(any(CheckAccessForIdsInputDto.class));
        assertThrowsInner(ForbiddenServiceException.class, () -> presentAPI.getRecord(RECORD_ID, FormatDto.MODS),
                "Calling getRecord should raise a forbidden exception");

        // Change the mock to not have the record
        CheckAccessForIdsOutputDto noRecordResponse = new CheckAccessForIdsOutputDto().nonExistingIds(List.of(RECORD_ID));
        doReturn(noRecordResponse).when(mockedLicenseClient).checkAccessForIds(any(CheckAccessForIdsInputDto.class));
        assertThrowsInner(NotFoundServiceException.class, () -> presentAPI.getRecord(RECORD_ID, FormatDto.MODS),
                "Calling getRecord should raise a not found exception");
    }

    /**
     * Assert that compares {@code expectedException} against the inner {@code Exception} (aka wrapped) for the
     * {@code Exception} thrown by running {@code executable}.
     * @param expectedException the expected inner {@code Exception}.
     * @param executable a code expected to throw an {@code Exception} wrapping an instance of {@code expectedException}.
     * @param message the message to show if the assert fails.
     */    
    private void assertThrowsInner(Class<? extends Exception> expectedException, Executable executable, String message) {
        try {
            executable.execute();
            fail(message + ": Expectected to fail with Exception with inner exception " + expectedException.getName());
        } catch (Throwable t) {
            assertTrue(t instanceof Exception,
                    message + ": Expected an Exception but got Throwable " + t.getClass().getName());
            Exception e = (Exception)t;
            assertEquals(expectedException, e.getCause().getClass(),
                    message + ": Expected Exception to expected inner Exception");
        }
    }

    @Test
    public void testMultiRecordsLicense() throws NoSuchFieldException, ServiceException, IOException {
        if (Resolver.getPathFromClasspath("internal_test_files") == null){
            return;
        }
        // No local.mods-prefix here. This is an oversight of the file based test-storage
        final String RECORD_ID1 = "40221e30-1414-11e9-8fb8-00505688346e.xml";
        final String RECORD_ID2 = "5cc1bea0-71fa-11e2-b31c-0016357f605f.xml";
        final String RECORD_ID3 = "3956d820-7b7d-11e6-b2b3-0016357f605f.xml";

        // Setup mock license that allow only 3 known records.
        DsPresentApiServiceImpl presentAPI = getMockedPresentAPI();
        DsLicenseClient mockedLicenseClient = mock(DsLicenseClient.class);
        CheckAccessForIdsOutputDto accessResponse = new CheckAccessForIdsOutputDto().accessIds(List.of(
                RECORD_ID1, RECORD_ID2, RECORD_ID3));
        doReturn(accessResponse).when(mockedLicenseClient).checkAccessForIds(any(CheckAccessForIdsInputDto.class));
        AccessUtil.licenseClient = mockedLicenseClient;
        StreamingOutput records = presentAPI.getRecords("dsfl", 0L, 1000L, FormatDto.MODS);
        assertEquals(3, PresentFacadeTest.countMETS(records),
                "3-specific-records-accepting license should return exactly 3 records");

        // Change the mock to not allow any record
        accessResponse = new CheckAccessForIdsOutputDto().accessIds(Collections.emptyList());
        doReturn(accessResponse).when(mockedLicenseClient).checkAccessForIds(any(CheckAccessForIdsInputDto.class));
        StreamingOutput noRecords = presentAPI.getRecords("dsfl", 0L, 1000L, FormatDto.MODS);
        assertEquals(0, PresentFacadeTest.countMETS(noRecords),
                "Zero accepting license should return exactly 0 records");
    }

    @Test
    public void testSingleRecordLicenseAllowAll() throws NoSuchFieldException, ServiceException, IOException {
        final String RECORD_ID = "local.mods:40221e30-1414-11e9-8fb8-00505688346e.xml";

        // Setup mock license that does not allow any records
        DsPresentApiServiceImpl presentAPI = getMockedPresentAPI();
        DsLicenseClient mockedLicenseClient = mock(DsLicenseClient.class);
        CheckAccessForIdsOutputDto noAccessResponse = new CheckAccessForIdsOutputDto().nonAccessIds(List.of(RECORD_ID));
        doReturn(noAccessResponse).when(mockedLicenseClient).checkAccessForIds(any(CheckAccessForIdsInputDto.class));
        AccessUtil.licenseClient = mockedLicenseClient;

        assertThrowsInner(ForbiddenServiceException.class, () -> presentAPI.getRecord(RECORD_ID, FormatDto.MODS),
                "Calling getRecord should not be allowed");

        // Set allowall=true and try again
        boolean oldAllowall = AccessUtil.licenseAllowAll;
        AccessUtil.licenseAllowAll = true;
        try {
            assertTrue(presentAPI.getRecord(RECORD_ID, FormatDto.MODS).contains("<mets:mets "),
                    "Extraction with allowall=true should work");
        } finally {
            // Clean up for next test
            AccessUtil.licenseAllowAll = oldAllowall;
        }
    }

    @Test
    @Tag("integration")
    public void testMultiRecordsLicenseAllowAll() throws NoSuchFieldException, ServiceException, IOException {
        if (Resolver.getPathFromClasspath("internal_test_files") == null){
            fail("Missing internal_test_files");
        }
        // Setup mock license that allows 1 record
        final String RECORD_ID1 = "40221e30-1414-11e9-8fb8-00505688346e.xml";

        // Setup mock license that allow only 1 known record.
        DsPresentApiServiceImpl presentAPI = getMockedPresentAPI();
        DsLicenseClient mockedLicenseClient = mock(DsLicenseClient.class);
        CheckAccessForIdsOutputDto accessResponse = new CheckAccessForIdsOutputDto().accessIds(List.of(RECORD_ID1));
        doReturn(accessResponse).when(mockedLicenseClient).checkAccessForIds(any(CheckAccessForIdsInputDto.class));
        AccessUtil.licenseClient = mockedLicenseClient;
        StreamingOutput records = presentAPI.getRecords("dsfl", 0L, 1000L, FormatDto.MODS);
        assertEquals(1, PresentFacadeTest.countMETS(records),
                "1-specific-record-accepting license should return exactly 1 records");

        // Set allowall=true and try again
        boolean oldAllowall = AccessUtil.licenseAllowAll;
        AccessUtil.licenseAllowAll = true;
        try {
            records = presentAPI.getRecords("dsfl", 0L, 1000L, FormatDto.MODS);
            assertTrue(PresentFacadeTest.countMETS(records) > 1,
                    "allowall should return more than 1 records");
        } finally {
            // Clean up for next test
            AccessUtil.licenseAllowAll = oldAllowall;
        }
    }

    /**
     * Basic mocking of the {@link DsPresentApiServiceImpl}. Callers should add further mocking.
     * @return a Mochito mock of {@code } DsPresentApiServiceImpl with {@code httpServletRequest}.
     */
    private static DsPresentApiServiceImpl getMockedPresentAPI() throws NoSuchFieldException {
        HttpServletMapping mockedMapping = mock(HttpServletMapping.class);
        doReturn("foo").when(mockedMapping).getMatchValue();

        HttpServletResponse mockedResponse = mock(HttpServletResponse.class);
        HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
        doReturn("GET").when(mockedRequest).getMethod();
        doReturn(mockedMapping).when(mockedRequest).getHttpServletMapping();
        doReturn(Collections.enumeration(List.of("foo/bar"))).when(mockedRequest).getHeaders("Accept");

        DsPresentApiServiceImpl presentAPI = spy(new DsPresentApiServiceImpl());
        setField(presentAPI, dk.kb.util.webservice.ImplBase.class.getDeclaredField("httpServletRequest"), mockedRequest);
        setField(presentAPI, dk.kb.util.webservice.ImplBase.class.getDeclaredField("httpServletResponse"), mockedResponse);
        return presentAPI;
    }
}