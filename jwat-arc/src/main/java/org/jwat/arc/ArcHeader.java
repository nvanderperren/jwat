/**
 * Java Web Archive Toolkit - Software to read and validate ARC, WARC
 * and GZip files. (http://jwat.org/)
 * Copyright 2011-2012 Netarkivet.dk (http://netarkivet.dk/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jwat.arc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.text.DateFormat;
import java.util.Date;

import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.ContentType;
import org.jwat.common.Diagnosis;
import org.jwat.common.Diagnostics;

public class ArcHeader {

    /** Associated WarcReader context.
     * Must be set prior to calling the various methods. */
    protected ArcReader reader;

    /** Diagnostics used to report diagnoses.
     * Must be set prior to calling the various methods. */
    protected Diagnostics<Diagnosis> diagnostics;

    /** ARC field parser used.
     * Must be set prior to calling the various methods. */
    protected ArcFieldParsers fieldParsers;

    /** ARC <code>DateFormat</code> as specified by the ARC specifications. */
    protected DateFormat warcDateFormat;

    /** Do the record fields comply in number with the one dictated by its
     *  version. */
    protected boolean hasCompliantFields = false;

    /** ARC record starting offset relative to the source arc file input
     *  stream. */
    protected long startOffset = -1L;

    /*
     * ARC header fields.
     */

    /** ARC record URL field string value. */
    public String urlStr;
    /** ARC record URL validated and converted to an <code>URI</code> object. */
    public URI urlUri;
    /** URI Scheme (lowercase). (filedesc, http, https, dns, etc.) */
    public String urlProtocol;

    /** ARC record IP-Address field string value. */
    public String ipAddressStr;
    /** IP-Address validated and converted to an <code>InetAddress</code> object. */
    public InetAddress inetAddress;

    /** ARC record archive-date field string value. */
    public String archiveDateStr;
    /** Archive date validated and converted to a <code>Date</code> object. */
    public Date archiveDate;

    /** ARC record content-type field string value. */
    public String contentTypeStr;
    /** Content-Type wrapper object with optional parameters. */
    public ContentType contentType;

    /** ARC record result-code field string value. */
    public String resultCodeStr;
    /** Result-code validated and converted into an <code>Integer</code> object. */
    public Integer resultCode;

    /** ARC record checksum field string value. */
    public String checksumStr;

    /** ARC record location field string value. */
    public String locationStr;

    /** ARC record offset field string value. */
    public String offsetStr;
    /** Offset validated and converted into a <code>Long</code> object. */
    public Long offset;

    /** ARC record filename field string value. */
    public String filenameStr;

    /** ARC record archive-length field string value. */
    protected String archiveLengthStr;
    /** Archive-length validated and converted into a <code>Long</code> object. */
    public Long archiveLength;

    public static ArcHeader initHeader(ArcWriter writer, Diagnostics<Diagnosis> diagnostics) {
        ArcHeader header = new ArcHeader();
        // TODO
        //header.major = 1;
        //header.minor = 0;
        header.fieldParsers = writer.fieldParsers;
        header.warcDateFormat = writer.arcDateFormat;
        header.diagnostics = diagnostics;
        return header;
    }

    public static ArcHeader initHeader(ArcReader reader, long startOffset, Diagnostics<Diagnosis> diagnostics) {
        ArcHeader header = new ArcHeader();
        header.reader = reader;
        header.fieldParsers = reader.fieldParsers;
        header.diagnostics = diagnostics;
        // This is only relevant for uncompressed sequentially read records
        header.startOffset = startOffset;
        return header;
    }

    public boolean parseHeader(ByteCountingPushBackInputStream in) throws IOException {
        startOffset = in.getConsumed();
        String recordLine = in.readLine();
        while ((recordLine != null) && (recordLine.length() == 0)) {
            startOffset = in.getConsumed();
            recordLine = in.readLine();
        }
        if (recordLine != null) {
            // debug
            System.out.println(recordLine);

            String[] fields = recordLine.split(" ", -1);
            parseHeaders(fields);
            // Compare to expected numbers of fields.
            // Extract mandatory version-independent header data.
            // TODO
            /*
            hasCompliantFields = (records.length
                              == versionBlock.descValidator.fieldNames.length);
            if(!hasCompliantFields) {
                diagnostics.addError(new Diagnosis(DiagnosisType.INVALID,
                        ARC_RECORD,
                        "URL record definition and record definition are not "
                                + "compliant"));
            }
            */
            return true;
        }
        return false;
    }

    public void parseHeaders(String[] fields) {
        if (fields.length >= ArcConstants.VERSION_1_BLOCK_FIELDS.length) {
            /*
             * Version 1.
             */
            urlStr = fields[ArcConstants.FN_IDX_URL];
            ipAddressStr = fields[ArcConstants.FN_IDX_IP_ADDRESS];
            archiveDateStr = fields[ArcConstants.FN_IDX_ARCHIVE_DATE];
            contentTypeStr = fields[ArcConstants.FN_IDX_CONTENT_TYPE];
            // Validate
            urlUri = fieldParsers.parseUri(urlStr, ArcConstants.FN_URL);
            if (urlUri != null) {
                urlProtocol = urlUri.getScheme();
                if (urlProtocol != null) {
                    urlProtocol = urlProtocol.toLowerCase();
                }
            }
            inetAddress = fieldParsers.parseIpAddress(ipAddressStr, ArcConstants.FN_IP_ADDRESS);
            archiveDate = fieldParsers.parseDate(archiveDateStr, ArcConstants.FN_ARCHIVE_DATE);
            contentType = fieldParsers.parseContentType(contentTypeStr, ArcConstants.FN_CONTENT_TYPE);
            // version.equals(ArcVersion.VERSION_2)
            if (fields.length >= ArcConstants.VERSION_2_BLOCK_FIELDS.length) {
                /*
                 *  Version 2.
                 */
                resultCodeStr = fields[ArcConstants.FN_IDX_RESULT_CODE];
                resultCode = fieldParsers.parseInteger(
                        resultCodeStr, ArcConstants.FN_RESULT_CODE, false);

                checksumStr = fields[ArcConstants.FN_IDX_CHECKSUM];
                checksumStr = fieldParsers.parseString(
                        checksumStr, ArcConstants.FN_CHECKSUM);

                locationStr = fields[ArcConstants.FN_IDX_LOCATION];
                locationStr = fieldParsers.parseString(
                        locationStr, ArcConstants.FN_LOCATION, true);

                offsetStr = fields[ArcConstants.FN_IDX_OFFSET];
                offset = fieldParsers.parseLong(
                        offsetStr, ArcConstants.FN_OFFSET);

                filenameStr = fields[ArcConstants.FN_IDX_FILENAME];
                filenameStr = reader.fieldParsers.parseString(
                        filenameStr, ArcConstants.FN_FILENAME);
            }
            archiveLengthStr = fields[fields.length - 1];
            archiveLength = reader.fieldParsers.parseLong(
                    archiveLengthStr, ArcConstants.FN_ARCHIVE_LENGTH);
        }
    }

    public void toStringBuilder(StringBuilder sb) {
        if (urlStr != null) {
            sb.append("url:").append(urlStr);
        }
        if (ipAddressStr != null) {
            sb.append(", ipAddress: ").append(ipAddressStr);
        }
        if (archiveDateStr != null) {
            sb.append(", archiveDate: ").append(archiveDateStr);
        }
        if (contentTypeStr != null) {
            sb.append(", contentType: ").append(contentTypeStr);
        }
        if (resultCode != null) {
            sb.append(", resultCode: ").append(resultCode);
        }
        if (checksumStr != null) {
            sb.append(", checksum: ").append(checksumStr);
        }
        if (locationStr != null) {
            sb.append(", location: ").append(locationStr);
        }
        if (offset != null) {
            sb.append(", offset: ").append(offset);
        }
        if (filenameStr != null) {
            sb.append(", fileName: ")
                .append(filenameStr);
            if (archiveLength != null) {
                sb.append(", length: ").append(archiveLength);
            }
        }
    }

}