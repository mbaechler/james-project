import java.io.InputStream
import java.util.Optional

import org.apache.commons.io.IOUtils
import org.apache.james.mdn.MDNReport
import org.apache.james.mdn.fields.ReportingUserAgent
import org.parboiled2.CharPredicate.{Alpha, Digit}
import org.parboiled2._
import shapeless.{HList, HNil}

/** **************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                 *
 * *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 * ***************************************************************/

object MDNReportParser {

  //def parse(is: InputStream, charset: String): Optional[MDNReport] = parse(IOUtils.toString(is, charset))


}

class MDNReportParser extends Parser {
  override def input: ParserInput = ???


  /*    disposition-notification-content =
                     [ reporting-ua-field CRLF ]
                     [ mdn-gateway-field CRLF ]
                     [ original-recipient-field CRLF ]
                     final-recipient-field CRLF
                     [ original-message-id-field CRLF ]
                     disposition-field CRLF
                     *( error-field CRLF )
                     *( extension-field CRLF )    */
  private def dispositionNotificationContent = rule {
    reportingUaField ~
    mdnGatewayField ~
    originalRecipientField ~
    finalRecipientField ~
    originalMessageIdField ~
    dispositionField ~
    zeroOrMore(errorField) ~
    zeroOrMore(extentionField)
  }

  case class ReportingUaField(uaName: String, uaProduct: Option[String])

  /*    reporting-ua-field = "Reporting-UA" ":" OWS ua-name OWS [
                                   ";" OWS ua-product OWS ]    */
  private def reportingUaField: Rule[HNil, ReportingUaField] = rule {
    ("Reporting-UA" ~ ":" ~ ows ~ capture(uaName) ~ ows ~ optional(ows ~ capture(uaProduct) ~ ows)) ~>
      ((a: String, b: Option[String]) => ReportingUaField(a, b))
  }

  //    ua-name = *text-no-semi
  def uaName = rule { zeroOrMore(textNoSemi) }

  /*    text-no-semi = %d1-9 /        ; "text" characters excluding NUL, CR,
                             %d11 / %d12 / %d14-58 / %d60-127      ; LF, or semi-colon    */
  def textNoSemi = rule {
    CharPredicate(1.toChar to 9.toChar) |
    ch(11) |
    ch(12) |
    CharPredicate(14.toChar to 58.toChar) |
    CharPredicate(60.toChar to 127.toChar)
  }

  //    ua-product = *([FWS] text)
  def uaProduct = rule { zeroOrMore(optional(fws) ~ text) }

  /*   text            =   %d1-9 /            ; Characters excluding CR
                                 %d11 /             ;  and LF
                                 %d12 /
                                 %d14-127   */
  def text = rule {
    CharPredicate(1.toChar to 9.toChar) |
      ch(11) |
      ch(12) |
      CharPredicate(14.toChar to 127.toChar)
  }

  /*    OWS = [CFWS]
            ; Optional whitespace.
            ; MDN generators SHOULD use "*WSP"
            ; (Typically a single space or nothing.
            ; It SHOULD be nothing at the end of a field.),
            ; unless an RFC 5322 "comment" is required.
            ;
            ; MDN parsers MUST parse it as "[CFWS]".    */
  private def ows = rule {
    optional(cfws)
  }

  /*    mdn-gateway-field = "MDN-Gateway" ":" OWS mta-name-type OWS
                                  ";" OWS mta-name    */
  def mdnGatewayField = rule {
    "MDN-Gateway" ~ ":" ~ ows ~ mtaNameType ~ ows ~ ";" ~ ows ~ mtaName
  }

  //    mta-name-type = Atom
  def mtaNameType = rule { atom }

  //    mta-name = *text
  def mtaName = rule { zeroOrMore(text) }

  /*    original-recipient-field =
                     "Original-Recipient" ":" OWS address-type OWS
                     ";" OWS generic-address OWS    */
  def originalRecipientField = rule {
    "Original-Recipient" ~ ":" ~ ows ~ addressType ~ ows ~ ";" ~ ows ~ genericAddress ~ ows
  }

  //    address-type = Atom
  def addressType = rule { atom }

  //    generic-address = *text
  def genericAddress = rule { zeroOrMore(text) }

  /*    final-recipient-field =
             "Final-Recipient" ":" OWS address-type OWS
             ";" OWS generic-address OWS    */
  def finalRecipientField = rule {
    "Final-Recipient" ~ ":" ~ ows ~ addressType ~ ows ~ ";" ~ ows ~ genericAddress ~ ows
  }

  //    original-message-id-field = "Original-Message-ID" ":" msg-id
  def originalMessageIdField = rule {
    "Original-Message-ID" ~ ":" ~ msgId
  }

  //    msg-id          =   [CFWS] "<" id-left "@" id-right ">" [CFWS]
  def msgId = rule { optional(cfws) ~ "<" ~ idLeft ~ "@" ~ idRight ~ ">" ~ optional(cfws) }

  //   id-left         =   dot-atom-text / obs-id-left
  def idLeft = rule { dotAtomText | obsIdLeft }

  //   obs-id-left     =   local-part
  def obsIdLeft = rule { localPart }

  //   obs-id-right    =   domain
  def idRight = rule { domain }

  /*    disposition-field =
                     "Disposition" ":" OWS disposition-mode OWS ";"
                     OWS disposition-type
                     [ OWS "/" OWS disposition-modifier
                     *( OWS "," OWS disposition-modifier ) ] OWS    */
  def dispositionField = rule {
    "Disposition" ~ ":" ~ ows ~ dispositionMode ~ ows ~ ";" ~
    ows ~ dispositionType ~
    optional(ows ~ "/" ~ ows ~ dispositionModifier ~
      zeroOrMore(ows ~ "," ~ ows ~ dispositionModifier)) ~ ows
  }

  //    disposition-mode = action-mode OWS "/" OWS sending-mode
  def dispositionMode = rule { actionMode ~ ows ~ "/" ~ ows ~ sendingMode }

  //    action-mode = "manual-action" / "automatic-action"
  def actionMode = rule { "manual-action" | "automatic-action" }

  //    sending-mode = "MDN-sent-manually" / "MDN-sent-automatically"
  def sendingMode = "MDN-sent-manually" ~ "MDN-sent-automatically"

  /*    disposition-type = "displayed" / "deleted" / "dispatched" /
                      "processed"    */
  def dispositionType = rule { "displayed" | "deleted" | "dispatched" | "processed" }

  //    disposition-modifier = "error" / disposition-modifier-extension
  def dispositionModifier = rule { "error" ~ dispositionModifierExtension }

  //    disposition-modifier-extension = Atom
  def dispositionModifierExtension = rule { atom }

  //    error-field = "Error" ":" *([FWS] text)
  def errorField = rule { "Error" ~ ":" ~ zeroOrMore(optional(fws) ~ text) }

  //    extension-field = extension-field-name ":" *([FWS] text)
  def extentionField = rule { extensionFieldName ~ ":" ~ zeroOrMore(optional(fws) ~ text) }

  //    extension-field-name = field-name
  def extensionFieldName = rule { fieldName }

  //   field-name      =   1*ftext
  def fieldName = rule { oneOrMore(ftext) }

  /*   ftext           =   %d33-57 /          ; Printable US-ASCII
                         %d59-126           ;  characters not including
                                            ;  ":".   */
  def ftext = rule {
    CharPredicate(33.toChar to 57.toChar) |
    CharPredicate(59.toChar to 126.toChar)
  }

  //   CFWS            =   (1*([FWS] comment) [FWS]) / FWS
  def cfws = rule { (oneOrMore(optional(fws) ~ comment) ~ fws) | fws }

  //   FWS             =   ([*WSP CRLF] 1*WSP) /  obs-FWS
  def fws = rule { (optional(zeroOrMore(wsp) ~ crlf) ~ oneOrMore(wsp)) | obsFWS }

  //         WSP            =  SP / HTAB
  def wsp = rule { sp | htab }

  //         SP             =  %x20
  def sp = rule { ch(0x20) }

  //         HTAB           =  %x09
  def htab = rule { ch(0x09) }

  //         CRLF           =  CR LF
  def crlf = rule { cr ~ lf }

  //         CR             =  %x0D
  def cr = rule { ch(0x0d) }

  //         LF             =  %x0A
  def lf = rule { ch(0x0a) }

  //   obs-FWS         =   1*WSP *(CRLF 1*WSP)
  def obsFWS = rule { oneOrMore(wsp) ~ zeroOrMore(crlf ~ oneOrMore(wsp)) }

  //   comment         =   "(" *([FWS] ccontent) [FWS] ")"
  def comment: Rule[HNil, HNil] = rule { "(" ~ zeroOrMore(optional(fws) ~ ccontent) ~ optional(fws) ~ ")" }

  //   ccontent        =   ctext / quoted-pair / comment
  def ccontent: Rule[HNil, HNil] = rule { ctext | quotedPair | comment }

  /*   ctext           =   %d33-39 /          ; Printable US-ASCII
                         %d42-91 /          ;  characters not including
                         %d93-126 /         ;  "(", ")", or "\"
                         obs-ctext   */
  def ctext = rule {
    CharPredicate(33.toChar to 39.toChar) |
    CharPredicate(42.toChar to 91.toChar) |
    CharPredicate(93.toChar to 126.toChar) |
    obsCText
  }

  //   obs-ctext       =   obs-NO-WS-CTL
  def obsCText = rule { obsNoWsCtl }

  /*   obs-NO-WS-CTL   =   %d1-8 /            ; US-ASCII control
                         %d11 /             ;  characters that do not
                         %d12 /             ;  include the carriage
                         %d14-31 /          ;  return, line feed, and
                         %d127              ;  white space characters   */
  def obsNoWsCtl = rule {
    CharPredicate(33.toChar to 39.toChar) |
    ch(11) |
    ch(12) |
    CharPredicate(14.toChar to 31.toChar) |
    ch(127)
  }

  //   quoted-pair     =   ("\" (VCHAR / WSP)) / obs-qp
  def quotedPair = rule { ("\\" ~ (vchar | wsp)) | obsQp }

  //         VCHAR          =  %x21-7E
  def vchar = rule { CharPredicate(21.toChar to 0x7e.toChar) }

  //   obs-qp          =   "\" (%d0 / obs-NO-WS-CTL / LF / CR)
  def obsQp = rule { "\\" ~ (ch(0xd0) | obsCText | lf | cr) }

  //   word            =   atom / quoted-string
  def word = rule { atom | quotedString }

  //    atom            =   [CFWS] 1*atext [CFWS]
  def atom = rule { optional(cfws) ~ oneOrMore(atext) ~ optional(cfws) }

  /*   atext           =   ALPHA / DIGIT /    ; Printable US-ASCII
                         "!" / "#" /        ;  characters not including
                         "$" / "%" /        ;  specials.  Used for atoms.
                         "&" / "'" /
                         "*" / "+" /
                         "-" / "/" /
                         "=" / "?" /
                         "^" / "_" /
                         "`" / "{" /
                         "|" / "}" /
                         "~"   */
  def atext = rule {
    alpha | digit |
    "!" | "#" |
    "$" | "%" |
    "&" | "'" |
    "*" | "+" |
    "-" | "/" |
    "=" | "?" |
    "^" | "_" |
    "`" | "{" |
    "|" | "}" |
    "~"
  }

  //         ALPHA          =  %x41-5A / %x61-7A   ; A-Z / a-z
  def alpha = rule {
    CharPredicate(0x41.toChar to 0x5a.toChar) |
    CharPredicate(0x61.toChar to 0x7a.toChar)
  }

  //         DIGIT          =  %x30-39
  def digit = rule { CharPredicate(0x30.toChar to 0x39.toChar) }

  /*   quoted-string   =   [CFWS]
                                 DQUOTE *([FWS] qcontent) [FWS] DQUOTE
                                 [CFWS]   */
  def quotedString = rule {
    optional(cfws) ~
    dquote ~ zeroOrMore(optional(fws) ~ qcontent) ~ optional(fws) ~ dquote ~
    optional(cfws)
  }

  //         DQUOTE         =  %x22
  def dquote = rule { ch(0x22) }

  //   qcontent        =   qtext / quoted-pair
  def qcontent: Rule[HNil, HList] = rule { qcontent | quotedPair }

  //   domain          =   dot-atom / domain-literal / obs-domain
  def domain = rule { dotAtom | domainLiteral | dotAtom }

  //   dot-atom        =   [CFWS] dot-atom-text [CFWS]
  def dotAtom = rule { optional(cfws) ~ dotAtomText ~ optional(cfws) }

  //   dot-atom-text   =   1*atext *("." 1*atext)
  def dotAtomText = rule { oneOrMore(atext) ~ zeroOrMore("." ~ oneOrMore(atext)) }

  //   domain-literal  =   [CFWS] "[" *([FWS] dtext) [FWS] "]" [CFWS]
  def domainLiteral = rule {
    optional(cfws) ~ "[" ~ zeroOrMore(optional(fws) ~ dtext) ~ optional(fws) ~ "]" ~ optional(cfws)
  }

  /*   dtext           =   %d33-90 /          ; Printable US-ASCII
                                 %d94-126 /         ;  characters not including
                                 obs-dtext          ;  "[", "]", or "\"   */
  def dtext = rule {
    CharPredicate(33.toChar to 90.toChar) |
    CharPredicate(94.toChar to 126.toChar) |
    obsDtext
  }

  //   obs-dtext       =   obs-NO-WS-CTL / quoted-pair
  def obsDtext = rule { obsNoWsCtl | quotedPair }

  //   obs-domain      =   atom *("." atom)
  def obsDomain = rule { atom ~ zeroOrMore("." ~ atom) }

  //   local-part      =   dot-atom / quoted-string / obs-local-part
  def localPart = rule { dotAtom | quotedString | obsLocalPart }

  //   obs-local-part  =   word *("." word)
  def obsLocalPart = rule { word ~ zeroOrMore("." ~ word) }

}