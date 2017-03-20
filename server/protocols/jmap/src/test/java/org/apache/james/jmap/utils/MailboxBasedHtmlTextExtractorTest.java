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

package org.apache.james.jmap.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.james.mailbox.tika.extractor.TikaTextExtractor;
import org.junit.Before;
import org.junit.Test;

public class MailboxBasedHtmlTextExtractorTest {

    private MailboxBasedHtmlTextExtractor textExtractor;

    @Before
    public void setUp() {
        textExtractor = new MailboxBasedHtmlTextExtractor(new TikaTextExtractor());
    }

    @Test
    public void toPlainTextShouldNotModifyPlainText() {
        String textWithoutHtml = "text without html";
        assertThat(textExtractor.toPlainText(textWithoutHtml)).isEqualTo(textWithoutHtml);
    }

    @Test
    public void toPlainTextShouldRemoveSimpleHtmlTag() {
        String html = "This is an <b>HTML</b> text !";
        String expectedPlainText = "This is an HTML text !";
        assertThat(textExtractor.toPlainText(html)).isEqualTo(expectedPlainText);
    }

    @Test
    public void toPlainTextShouldReplaceSkipLine() {
        String html = "<p>This is an<br/>HTML text !</p>";
        String expectedPlainText = "This is an\nHTML text !\n";
        assertThat(textExtractor.toPlainText(html)).isEqualTo(expectedPlainText);
    }

    @Test
    public void toPlainTextShouldSkipLinesBetweenParagraph() {
        String html = "<p>para1</p><p>para2</p>";
        String expectedPlainText = "para1\npara2\n";
        assertThat(textExtractor.toPlainText(html)).isEqualTo(expectedPlainText);
    }

    @Test
    public void nonClosedHtmlShouldBeTranslated() {
        String html = "This is an <b>HTML text !";
        String expectedPlainText = "This is an HTML text !";
        assertThat(textExtractor.toPlainText(html)).isEqualTo(expectedPlainText);
    }

    @Test
    public void brokenHtmlShouldBeTranslatedUntilTheBrokenBalise() {
        String html = "This is an <b>HTML</b missing missing missing !";
        String expectedPlainText = "This is an HTML";
        assertThat(textExtractor.toPlainText(html)).isEqualTo(expectedPlainText);
    }

    @Test
    public void toPlainTextShouldWorkWithMoreComplexHTML() throws Exception {
        String html = IOUtils.toString(ClassLoader.getSystemResource("example.html"), StandardCharsets.UTF_8);
        String expectedPlainText = "\n" +
            "    Why a new Logo?\n" +
            "\n" +
            "\n" +
            "\n" +
            "    We are happy with our current logo, but for the\n" +
            "        upcoming James Server 3.0 release, we would like to\n" +
            "        give our community the opportunity to create a new image for James.\n" +
            "\n" +
            "\n" +
            "\n" +
            "    Don't be shy, take your inkscape and gimp, and send us on\n" +
            "        the James Server User mailing list\n" +
            "        your creations. We will publish them on this page.\n" +
            "\n" +
            "\n" +
            "\n" +
            "    We need an horizontal logo (100p height) to be show displayed on the upper\n" +
            "        left corner of this page, an avatar (48x48p) to be used on a Twitter stream for example.\n" +
            "        The used fonts should be redistributable (or commonly available on Windows and Linux).\n" +
            "        The chosen logo should be delivered in SVG format.\n" +
            "        We also like the Apache feather.\n" +
            "\n" +
            "\n" +
            "\n";
        assertThat(textExtractor.toPlainText(html)).isEqualTo(expectedPlainText);
    }

}
