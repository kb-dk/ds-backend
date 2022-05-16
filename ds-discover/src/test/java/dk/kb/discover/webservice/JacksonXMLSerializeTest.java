package dk.kb.discover.webservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import dk.kb.discover.model.v1.BookDto;
import org.junit.jupiter.api.Test;

import javax.xml.bind.annotation.XmlRootElement;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
class JacksonXMLSerializeTest {

    @Test
    void testJacksonXMLSerializationCustomObject() throws JsonProcessingException {
        XmlMapper mapper = new XmlMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String xmlString = mapper.writeValueAsString(new Sample("Some_ID", "Some content"));

        assertEquals("<sample id=\"Some_ID\">\n" +
                     "  <content>Some content</content>\n" +
                     "</sample>\n", xmlString);
    }

    @Test
    void testJacksonXMLSerializationBook() throws JsonProcessingException {
        XmlMapper mapper = new XmlMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String xmlString = mapper.writeValueAsString(new BookDto().id("Some_ID").title("Some title"));

        assertEquals("<book id=\"Some_ID\">\n" +
                     "  <title>Some title</title>\n" +
                     "</book>\n", xmlString);
    }


    //@XmlRootElement(name = "sample")
    @JacksonXmlRootElement(localName = "sample")
    public static class Sample {
//        @XmlAttribute(name = "id")
//        @SerializedName("id")
        @JacksonXmlProperty(isAttribute = true, localName="id")
        private String theIDField;

        //@JsonProperty("content") // This seems to be the defining annotation to get proper element name in the XML
        @JacksonXmlProperty(localName="content")
        private String contentField;


        public Sample(String id, String content) {
            this.theIDField = id;
            this.contentField = content;
        }

        public String getTheIDField() {
            return theIDField;
        }

        public void setTheIDField(String theIDField) {
            this.theIDField = theIDField;
        }

        public String getContent() {
            return contentField;
        }

        public void setContent(String content) {
            this.contentField = content;
        }
    }

}