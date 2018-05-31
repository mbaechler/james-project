/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.protocols.imap.utils;

import static java.util.Calendar.APRIL;
import static java.util.Calendar.AUGUST;
import static java.util.Calendar.DECEMBER;
import static java.util.Calendar.FEBRUARY;
import static java.util.Calendar.JANUARY;
import static java.util.Calendar.JULY;
import static java.util.Calendar.JUNE;
import static java.util.Calendar.MARCH;
import static java.util.Calendar.MAY;
import static java.util.Calendar.NOVEMBER;
import static java.util.Calendar.OCTOBER;
import static java.util.Calendar.SEPTEMBER;
import static org.apache.james.protocols.imap.utils.DecoderUtils.decodeDateTime;
import static org.apache.james.protocols.imap.utils.DecoderUtils.decodeDigit;
import static org.apache.james.protocols.imap.utils.DecoderUtils.decodeMonth;
import static org.apache.james.protocols.imap.utils.DecoderUtils.decodeNumber;
import static org.apache.james.protocols.imap.utils.DecoderUtils.decodeYear;
import static org.apache.james.protocols.imap.utils.DecoderUtils.decodeZone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.mail.Flags;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.james.protocols.imap.DecodingException;
import org.junit.Test;

public class DecoderUtilsTest {

    private static final String EXTENSION_FLAG = "\\Extension";

    private static final String A_CUSTOM_FLAG = "Another";


    
    @Test
    public void testSetRecentFlag() {
        Flags flags = new Flags();
        try {
            DecoderUtils.setFlag("\\Recent", flags);
            fail("DecodingException was expect");
        } catch (DecodingException e) {
            // expected
        }
    }

    @Test
    public void testSetOtherFlag() throws Exception {
        Flags flags = new Flags();
        DecoderUtils.setFlag(A_CUSTOM_FLAG, flags);
        assertThat(flags
            .contains(A_CUSTOM_FLAG)).withFailMessage("Unknown flags should be added").isTrue();
    }

    @Test
    public void testExtensionFlag() throws Exception {
        Flags flags = new Flags();
        DecoderUtils.setFlag(EXTENSION_FLAG, flags);
        assertThat(flags
            .contains(EXTENSION_FLAG)).withFailMessage("Extension flags should be added").isTrue();
    }

    @Test
    public void testBadDateTime() throws Exception {
        checkDateTime(null);
        checkDateTime("");
        checkDateTime("This is a string long enough to be too big");
        checkDateTime("1");
        checkDateTime("12");
        checkDateTime("123");
        checkDateTime("1234");
        checkDateTime("12345");
        checkDateTime("123456");
        checkDateTime("1234567");
        checkDateTime("12345678");
        checkDateTime("123456789");
        checkDateTime("1234567890");
        checkDateTime("12345678901");
        checkDateTime("123456789012");
        checkDateTime("1234567890123");
        checkDateTime("12345678901234");
        checkDateTime("123456789012345");
        checkDateTime("1234567890123456");
        checkDateTime("12345678901234567");
        checkDateTime("123456789012345678");
        checkDateTime("1234567890123456789");
        checkDateTime("12345678901234567890");
        checkDateTime("123456789012345678901");
        checkDateTime("1234567890123456789012");
        checkDateTime("12345678901234567890123");
        checkDateTime("123456789012345678901234");
        checkDateTime("1234567890123456789012345");
        checkDateTime("12345678901234567890123456");
        checkDateTime("123456789012345678901234567");
    }

    private void checkDateTime(String datetime) throws Exception {
        try {
            DecoderUtils.decodeDateTime(datetime);
            fail("Bad date-time" + datetime);
        } catch (DecodingException e) {
            // expected
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSimpleDecodeDateTime() throws Exception {
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +0000").toGMTString()).isEqualTo("21 Oct 1972 20:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +0100").toGMTString()).isEqualTo("21 Oct 1972 19:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +0200").toGMTString()).isEqualTo("21 Oct 1972 18:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +0300").toGMTString()).isEqualTo("21 Oct 1972 17:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +0400").toGMTString()).isEqualTo("21 Oct 1972 16:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +0500").toGMTString()).isEqualTo("21 Oct 1972 15:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +0600").toGMTString()).isEqualTo("21 Oct 1972 14:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +0700").toGMTString()).isEqualTo("21 Oct 1972 13:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +0800").toGMTString()).isEqualTo("21 Oct 1972 12:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +0900").toGMTString()).isEqualTo("21 Oct 1972 11:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +1000").toGMTString()).isEqualTo("21 Oct 1972 10:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +1100").toGMTString()).isEqualTo("21 Oct 1972 09:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +1200").toGMTString()).isEqualTo("21 Oct 1972 08:00:00 GMT");

        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +1000").toGMTString()).isEqualTo("21 Oct 1972 10:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 -0100").toGMTString()).isEqualTo("21 Oct 1972 21:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 -0200").toGMTString()).isEqualTo("21 Oct 1972 22:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 -0300").toGMTString()).isEqualTo("21 Oct 1972 23:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 -0400").toGMTString()).isEqualTo("22 Oct 1972 00:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 -0500").toGMTString()).isEqualTo("22 Oct 1972 01:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 -0600").toGMTString()).isEqualTo("22 Oct 1972 02:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 -0700").toGMTString()).isEqualTo("22 Oct 1972 03:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 -0800").toGMTString()).isEqualTo("22 Oct 1972 04:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 -0900").toGMTString()).isEqualTo("22 Oct 1972 05:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 -1000").toGMTString()).isEqualTo("22 Oct 1972 06:00:00 GMT");

        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 +0030").toGMTString()).isEqualTo("21 Oct 1972 19:30:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 -0030").toGMTString()).isEqualTo("21 Oct 1972 20:30:00 GMT");

        assertThat(decodeDateTime(
            "21-Oct-1972 20:00:00 -1000").toGMTString()).isEqualTo("22 Oct 1972 06:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 06:00:00 +1000").toGMTString()).isEqualTo("20 Oct 1972 20:00:00 GMT");
        assertThat(decodeDateTime(
            "21-Oct-1972 06:00:00 -1000").toGMTString()).isEqualTo("21 Oct 1972 16:00:00 GMT");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testAppleMailPrependsZeroNotSpace() throws Exception {
        assertThat(decodeDateTime(
            "09-Apr-2008 15:17:51 +0200").toGMTString()).isEqualTo("9 Apr 2008 13:17:51 GMT");
    }

    @Test
    public void testDecodeDateTime() throws Exception {
        runTimeZoneTest(TimeZone.getTimeZone("GMT+0"));
        runTimeZoneTest(TimeZone.getTimeZone("GMT+1"));
        runTimeZoneTest(TimeZone.getTimeZone("GMT-1"));
        runTimeZoneTest(TimeZone.getTimeZone("GMT+2"));
        runTimeZoneTest(TimeZone.getTimeZone("GMT-2"));
        runTimeZoneTest(TimeZone.getTimeZone("GMT+3"));
        runTimeZoneTest(TimeZone.getTimeZone("GMT-3"));
        runTimeZoneTest(TimeZone.getTimeZone("GMT+11"));
        runTimeZoneTest(TimeZone.getTimeZone("GMT-11"));
        runTimeZoneTest(TimeZone.getTimeZone("GMT+1030"));
        runTimeZoneTest(TimeZone.getTimeZone("GMT-1030"));
        runTimeZoneTest(TimeZone.getTimeZone("GMT+0045"));
        runTimeZoneTest(TimeZone.getTimeZone("GMT-0045"));
    }

    private void runTimeZoneTest(TimeZone zone) throws Exception {
        runDecodeDateTimeTest(new Date(10000000), zone);
        runDecodeDateTimeTest(new Date(100000000), zone);
        runDecodeDateTimeTest(new Date(1000000000), zone);
        runDecodeDateTimeTest(new Date(10000000000L), zone);
        runDecodeDateTimeTest(new Date(100000000000L), zone);
        runDecodeDateTimeTest(new Date(1000000000000L), zone);
        runDecodeDateTimeTest(new Date(1194168899658L), zone);
        runDecodeDateTimeTest(new Date(1912093499271L), zone);
        runDecodeDateTimeTest(new Date(1526720308423L), zone);
        runDecodeDateTimeTest(new Date(1487487260757L), zone);
        runDecodeDateTimeTest(new Date(1584040720026L), zone);
        runDecodeDateTimeTest(new Date(1983293490921L), zone);
        runDecodeDateTimeTest(new Date(1179806572669L), zone);
        runDecodeDateTimeTest(new Date(1194038035064L), zone);
        runDecodeDateTimeTest(new Date(1057865248366L), zone);
        runDecodeDateTimeTest(new Date(1052797936633L), zone);
        runDecodeDateTimeTest(new Date(1075268253439L), zone);
        runDecodeDateTimeTest(new Date(1033938440306L), zone);
        runDecodeDateTimeTest(new Date(1031614051298L), zone);
        runDecodeDateTimeTest(new Date(1059929345305L), zone);
        runDecodeDateTimeTest(new Date(1162582627756L), zone);
        runDecodeDateTimeTest(new Date(1185747232134L), zone);
        runDecodeDateTimeTest(new Date(1151301821303L), zone);
        runDecodeDateTimeTest(new Date(1116091684805L), zone);
        runDecodeDateTimeTest(new Date(1159599194961L), zone);
        runDecodeDateTimeTest(new Date(1222523245646L), zone);
        runDecodeDateTimeTest(new Date(1219556266559L), zone);
        runDecodeDateTimeTest(new Date(1290015730272L), zone);
        runDecodeDateTimeTest(new Date(1221694598854L), zone);
        runDecodeDateTimeTest(new Date(1212132783343L), zone);
        runDecodeDateTimeTest(new Date(1221761134897L), zone);
        runDecodeDateTimeTest(new Date(1270941981377L), zone);
        runDecodeDateTimeTest(new Date(1224491731327L), zone);
        runDecodeDateTimeTest(new Date(1268571556436L), zone);
        runDecodeDateTimeTest(new Date(1246838821081L), zone);
        runDecodeDateTimeTest(new Date(1226795970848L), zone);
        runDecodeDateTimeTest(new Date(1260254185119L), zone);
    }

    private void runDecodeDateTimeTest(Date date, TimeZone zone)
            throws Exception {
        dateDecode(formatAsImap(date, zone), zone);
    }

    private void dateDecode(String in, TimeZone zone) throws Exception {
        Date date = DecoderUtils.decodeDateTime(in);
        String out = formatAsImap(date, zone);
        assertThat(out).withFailMessage("Round trip").isEqualTo(in);
    }

    private String formatAsImap(Date date, TimeZone zone) {
        assertThat(date).isNotNull();
        FastDateFormat format = FastDateFormat.getInstance(
                "dd-MMM-yyyy hh:mm:ss Z", zone, Locale.US);
        String out = format.format(date);
        if (out.charAt(0) == '0') {
            out = ' ' + out.substring(1, out.length());
        }
        return out;
    }

    @Test
    public void testDecodeDigit() throws Exception {
        assertThat(decodeDigit('0')).isEqualTo(0);
        assertThat(decodeDigit('1')).isEqualTo(1);
        assertThat(decodeDigit('2')).isEqualTo(2);
        assertThat(decodeDigit('3')).isEqualTo(3);
        assertThat(decodeDigit('4')).isEqualTo(4);
        assertThat(decodeDigit('5')).isEqualTo(5);
        assertThat(decodeDigit('6')).isEqualTo(6);
        assertThat(decodeDigit('7')).isEqualTo(7);
        assertThat(decodeDigit('8')).isEqualTo(8);
        assertThat(decodeDigit('9')).isEqualTo(9);

        try {
            DecoderUtils.decodeDigit('/');
            fail("/ is not a digit");
        } catch (DecodingException e) {
            // expected
        }

        try {
            DecoderUtils.decodeDigit(':');
            fail(": is not a digit");
        } catch (DecodingException e) {
            // expected
        }
    }

    @Test
    public void testDecodeMonth() throws Exception {
        assertThat(decodeMonth('J', 'A', 'N')).isEqualTo(JANUARY);
        assertThat(decodeMonth('j', 'a', 'n')).isEqualTo(JANUARY);
        assertThat(decodeMonth('F', 'E', 'B')).isEqualTo(FEBRUARY);
        assertThat(decodeMonth('f', 'e', 'b')).isEqualTo(FEBRUARY);
        assertThat(decodeMonth('M', 'A', 'R')).isEqualTo(MARCH);
        assertThat(decodeMonth('m', 'a', 'r')).isEqualTo(MARCH);
        assertThat(decodeMonth('A', 'P', 'R')).isEqualTo(APRIL);
        assertThat(decodeMonth('a', 'p', 'r')).isEqualTo(APRIL);
        assertThat(decodeMonth('M', 'A', 'Y')).isEqualTo(MAY);
        assertThat(decodeMonth('m', 'a', 'y')).isEqualTo(MAY);
        assertThat(decodeMonth('J', 'U', 'N')).isEqualTo(JUNE);
        assertThat(decodeMonth('j', 'u', 'n')).isEqualTo(JUNE);
        assertThat(decodeMonth('J', 'U', 'L')).isEqualTo(JULY);
        assertThat(decodeMonth('j', 'u', 'l')).isEqualTo(JULY);
        assertThat(decodeMonth('A', 'U', 'G')).isEqualTo(AUGUST);
        assertThat(decodeMonth('a', 'u', 'g')).isEqualTo(AUGUST);
        assertThat(decodeMonth('S', 'E', 'P')).isEqualTo(SEPTEMBER);
        assertThat(decodeMonth('s', 'e', 'p')).isEqualTo(SEPTEMBER);
        assertThat(decodeMonth('O', 'C', 'T')).isEqualTo(OCTOBER);
        assertThat(decodeMonth('o', 'c', 't')).isEqualTo(OCTOBER);
        assertThat(decodeMonth('N', 'O', 'V')).isEqualTo(NOVEMBER);
        assertThat(decodeMonth('n', 'o', 'v')).isEqualTo(NOVEMBER);
        assertThat(decodeMonth('D', 'E', 'C')).isEqualTo(DECEMBER);
        assertThat(decodeMonth('d', 'e', 'c')).isEqualTo(DECEMBER);
    }

    @Test
    public void testRejectBogusMonths() throws Exception {
        checkReject('N', 'O', 'C');
        checkReject('A', 'N', 'T');
        checkReject('Z', 'Z', 'Z');
        checkReject('S', 'I', 'P');
        checkReject('D', 'E', 'P');
    }

    private void checkReject(char one, char two, char three) {
        try {
            DecoderUtils.decodeMonth(one, two, three);
            fail(one + two + three + "is not a month");
        } catch (DecodingException e) {
            // expected
        }
    }

    @Test
    public void testDecodeYear() throws Exception {
        assertThat(decodeYear('1', '9', '9', '9')).isEqualTo(1999);
        assertThat(decodeYear('0', '7', '4', '7')).isEqualTo(747);
        assertThat(decodeYear('2', '5', '2', '5')).isEqualTo(2525);
        assertThat(decodeYear('5', '6', '7', '8')).isEqualTo(5678);
        assertThat(decodeYear('2', '4', '5', '3')).isEqualTo(2453);
        assertThat(decodeYear('2', '0', '0', '0')).isEqualTo(2000);
        assertThat(decodeYear('2', '0', '0', '7')).isEqualTo(2007);
        assertThat(decodeYear('2', '0', '0', '8')).isEqualTo(2008);
        assertThat(decodeYear('2', '0', '1', '0')).isEqualTo(2010);
        assertThat(decodeYear('2', '0', '2', '0')).isEqualTo(2020);
    }

    @Test
    public void testRejectBogusYear() throws Exception {
        checkRejectYear('D', '0', '2', '3');
        checkRejectYear('1', 'A', '2', '3');
        checkRejectYear('1', '5', 'B', '3');
        checkRejectYear('9', '8', '2', 'C');
        checkRejectYear('S', 'A', 'F', 'd');
    }

    private void checkRejectYear(char one, char two, char three, char four) {
        try {
            DecoderUtils.decodeYear(one, two, three, four);
            fail(one + two + three + four + "is not a month");
        } catch (DecodingException e) {
            // expected
        }
    }

    @Test
    public void testDecodeZone() throws Exception {
        assertThat(decodeZone('+', '0', '0', '0', '0')).isEqualTo(0);
        assertThat(decodeZone('+', '0', '1', '0', '0')).isEqualTo(100);
        assertThat(decodeZone('+', '0', '2', '0', '0')).isEqualTo(200);
        assertThat(decodeZone('+', '0', '3', '0', '0')).isEqualTo(300);
        assertThat(decodeZone('+', '0', '4', '0', '0')).isEqualTo(400);
        assertThat(decodeZone('+', '0', '5', '0', '0')).isEqualTo(500);
        assertThat(decodeZone('+', '0', '6', '0', '0')).isEqualTo(600);
        assertThat(decodeZone('+', '0', '7', '0', '0')).isEqualTo(700);
        assertThat(decodeZone('+', '0', '8', '0', '0')).isEqualTo(800);
        assertThat(decodeZone('+', '0', '9', '0', '0')).isEqualTo(900);
        assertThat(decodeZone('+', '1', '0', '0', '0')).isEqualTo(1000);
        assertThat(decodeZone('+', '1', '1', '0', '0')).isEqualTo(1100);
        assertThat(decodeZone('+', '1', '2', '0', '0')).isEqualTo(1200);
        assertThat(decodeZone('+', '0', '0', '3', '0')).isEqualTo(30);
        assertThat(decodeZone('+', '0', '1', '3', '0')).isEqualTo(130);
        assertThat(decodeZone('+', '0', '2', '3', '0')).isEqualTo(230);
        assertThat(decodeZone('+', '0', '3', '3', '0')).isEqualTo(330);
        assertThat(decodeZone('+', '0', '4', '3', '0')).isEqualTo(430);
        assertThat(decodeZone('+', '0', '5', '3', '0')).isEqualTo(530);
        assertThat(decodeZone('+', '0', '6', '3', '0')).isEqualTo(630);
        assertThat(decodeZone('+', '0', '7', '3', '0')).isEqualTo(730);
        assertThat(decodeZone('+', '0', '8', '3', '0')).isEqualTo(830);
        assertThat(decodeZone('+', '0', '9', '3', '0')).isEqualTo(930);
        assertThat(decodeZone('+', '1', '0', '3', '0')).isEqualTo(1030);
        assertThat(decodeZone('+', '1', '1', '3', '0')).isEqualTo(1130);
        assertThat(decodeZone('+', '1', '1', '1', '1')).isEqualTo(1111);
        assertThat(decodeZone('-', '0', '0', '0', '0')).isEqualTo(0);
        assertThat(decodeZone('-', '0', '1', '0', '0')).isEqualTo(-100);
        assertThat(decodeZone('-', '0', '2', '0', '0')).isEqualTo(-200);
        assertThat(decodeZone('-', '0', '3', '0', '0')).isEqualTo(-300);
        assertThat(decodeZone('-', '0', '4', '0', '0')).isEqualTo(-400);
        assertThat(decodeZone('-', '0', '5', '0', '0')).isEqualTo(-500);
        assertThat(decodeZone('-', '0', '6', '0', '0')).isEqualTo(-600);
        assertThat(decodeZone('-', '0', '7', '0', '0')).isEqualTo(-700);
        assertThat(decodeZone('-', '0', '8', '0', '0')).isEqualTo(-800);
        assertThat(decodeZone('-', '0', '9', '0', '0')).isEqualTo(-900);
        assertThat(decodeZone('-', '1', '0', '0', '0')).isEqualTo(-1000);
        assertThat(decodeZone('-', '1', '1', '0', '0')).isEqualTo(-1100);
        assertThat(decodeZone('-', '1', '2', '0', '0')).isEqualTo(-1200);
        assertThat(decodeZone('-', '0', '0', '3', '0')).isEqualTo(-30);
        assertThat(decodeZone('-', '0', '1', '3', '0')).isEqualTo(-130);
        assertThat(decodeZone('-', '0', '2', '3', '0')).isEqualTo(-230);
        assertThat(decodeZone('-', '0', '3', '3', '0')).isEqualTo(-330);
        assertThat(decodeZone('-', '0', '4', '3', '0')).isEqualTo(-430);
        assertThat(decodeZone('-', '0', '5', '3', '0')).isEqualTo(-530);
        assertThat(decodeZone('-', '0', '6', '3', '0')).isEqualTo(-630);
        assertThat(decodeZone('-', '0', '7', '3', '0')).isEqualTo(-730);
        assertThat(decodeZone('-', '0', '8', '3', '0')).isEqualTo(-830);
        assertThat(decodeZone('-', '0', '9', '3', '0')).isEqualTo(-930);
        assertThat(decodeZone('-', '1', '0', '3', '0')).isEqualTo(-1030);
        assertThat(decodeZone('-', '1', '1', '3', '0')).isEqualTo(-1130);
        assertThat(decodeZone('-', '1', '1', '1', '1')).isEqualTo(-1111);

    }

    @Test
    public void testBogusZones() throws Exception {
        checkRejectZone(" 0000");
        checkRejectZone(" GMT ");
        checkRejectZone("DANG!");
        checkRejectZone("+a000");
        checkRejectZone("+0b00");
        checkRejectZone("+00c0");
        checkRejectZone("+000d");
        checkRejectZone("-a000");
        checkRejectZone("-0b00");
        checkRejectZone("-00c0");
        checkRejectZone("-000d");
    }

    private void checkRejectZone(String zone) {
        try {
            DecoderUtils.decodeZone(zone.charAt(0), zone.charAt(1), zone
                    .charAt(2), zone.charAt(3), zone.charAt(4));
            fail(zone + "is not a timezone");
        } catch (DecodingException e) {
            // expected
        }
    }

    @Test
    public void testIsSimpleDigit() throws Exception {
        assertThat(DecoderUtils.isSimpleDigit('0')).isTrue();
        assertThat(DecoderUtils.isSimpleDigit('1')).isTrue();
        assertThat(DecoderUtils.isSimpleDigit('2')).isTrue();
        assertThat(DecoderUtils.isSimpleDigit('3')).isTrue();
        assertThat(DecoderUtils.isSimpleDigit('4')).isTrue();
        assertThat(DecoderUtils.isSimpleDigit('5')).isTrue();
        assertThat(DecoderUtils.isSimpleDigit('6')).isTrue();
        assertThat(DecoderUtils.isSimpleDigit('7')).isTrue();
        assertThat(DecoderUtils.isSimpleDigit('8')).isTrue();
        assertThat(DecoderUtils.isSimpleDigit('9')).isTrue();

        assertThat(DecoderUtils.isSimpleDigit('/')).isFalse();
        assertThat(DecoderUtils.isSimpleDigit('.')).isFalse();
        assertThat(DecoderUtils.isSimpleDigit('-')).isFalse();
        assertThat(DecoderUtils.isSimpleDigit('+')).isFalse();
        assertThat(DecoderUtils.isSimpleDigit(',')).isFalse();
        assertThat(DecoderUtils.isSimpleDigit('*')).isFalse();
        assertThat(DecoderUtils.isSimpleDigit(':')).isFalse();
        assertThat(DecoderUtils.isSimpleDigit(';')).isFalse();
        assertThat(DecoderUtils.isSimpleDigit('<')).isFalse();
        assertThat(DecoderUtils.isSimpleDigit('=')).isFalse();
        assertThat(DecoderUtils.isSimpleDigit('>')).isFalse();
        assertThat(DecoderUtils.isSimpleDigit('A')).isFalse();
        assertThat(DecoderUtils.isSimpleDigit('B')).isFalse();
    }

    @Test
    public void testDecodeNumber() throws Exception {
        assertThat(decodeNumber('0', '1')).isEqualTo(1);
        assertThat(decodeNumber('0', '2')).isEqualTo(2);
        assertThat(decodeNumber('0', '3')).isEqualTo(3);
        assertThat(decodeNumber('0', '4')).isEqualTo(4);
        assertThat(decodeNumber('0', '5')).isEqualTo(5);
        assertThat(decodeNumber('0', '6')).isEqualTo(6);
        assertThat(decodeNumber('0', '7')).isEqualTo(7);
        assertThat(decodeNumber('0', '8')).isEqualTo(8);
        assertThat(decodeNumber('0', '9')).isEqualTo(9);
        assertThat(decodeNumber('1', '9')).isEqualTo(19);
        assertThat(decodeNumber('2', '8')).isEqualTo(28);
        assertThat(decodeNumber('3', '7')).isEqualTo(37);
        assertThat(decodeNumber('4', '6')).isEqualTo(46);
        assertThat(decodeNumber('5', '5')).isEqualTo(55);
        assertThat(decodeNumber('6', '4')).isEqualTo(64);
        assertThat(decodeNumber('7', '3')).isEqualTo(73);
        assertThat(decodeNumber('8', '2')).isEqualTo(82);
        assertThat(decodeNumber('9', '1')).isEqualTo(91);
    }

    @Test
    public void testRejectNumber() throws Exception {
        checkRejectNumber("A1");
        checkRejectNumber("1A");
        checkRejectNumber("AA");
    }

    private void checkRejectNumber(String number) {
        try {
            DecoderUtils.decodeNumber(number.charAt(0), number.charAt(1));
            fail(number + "is not a number");
        } catch (DecodingException e) {
            // expected
        }
    }
}
