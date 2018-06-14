package org.apache.james.transport.matchers.dlp;

import static org.apache.mailet.base.MailAddressFixture.ANY_AT_JAMES;
import static org.apache.mailet.base.MailAddressFixture.JAMES_APACHE_ORG;
import static org.apache.mailet.base.MailAddressFixture.JAMES_APACHE_ORG_DOMAIN;
import static org.apache.mailet.base.MailAddressFixture.OTHER_AT_JAMES;
import static org.apache.mailet.base.MailAddressFixture.RECIPIENT1;
import static org.apache.mailet.base.MailAddressFixture.RECIPIENT2;
import static org.apache.mailet.base.MailAddressFixture.RECIPIENT3;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.regex.Pattern;

import javax.mail.MessagingException;

import org.apache.james.core.Domain;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.james.dlp.api.DLPConfigurationItem.Id;
import org.apache.mailet.base.test.FakeMail;
import org.junit.jupiter.api.Test;

class DlpTest {

    private static final DlpRulesLoader MATCH_ALL_FOR_ALL_DOMAINS = (Domain domain) -> DlpDomainRules.matchAll();
    private static final DlpRulesLoader MATCH_NOTHING_FOR_ALL_DOMAINS = (Domain domain) -> DlpDomainRules.matchNothing();

    private static DlpRulesLoader asRulesLoaderFor(Domain domain, DlpDomainRules rules) {
        return (Domain d) -> Optional
                .of(d)
                .filter(domain::equals)
                .map(ignore -> rules)
                .orElse(DlpDomainRules.matchNothing());
    }

    @Test
    void matchShouldReturnEmptyWhenNoRecipient() throws MessagingException {
        Dlp dlp = new Dlp(MATCH_ALL_FOR_ALL_DOMAINS);

        FakeMail mail = FakeMail.builder().sender(RECIPIENT1).build();

        assertThat(dlp.match(mail)).isEmpty();
    }

    @Test
    void matchShouldReturnEmptyWhenNoSender() throws MessagingException {
        Dlp dlp = new Dlp(MATCH_ALL_FOR_ALL_DOMAINS);

        FakeMail mail = FakeMail.builder().recipient(RECIPIENT1).build();

        assertThat(dlp.match(mail)).isEmpty();
    }

    @Test
    void matchShouldThrowOnNullMail() {
        Dlp dlp = new Dlp(MATCH_ALL_FOR_ALL_DOMAINS);

        assertThatThrownBy(() -> dlp.match(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void matchShouldReturnEmptyWhenNoRuleMatch() throws MessagingException {
        Dlp dlp = new Dlp(MATCH_NOTHING_FOR_ALL_DOMAINS);

        FakeMail mail = FakeMail.builder()
            .sender(ANY_AT_JAMES)
            .recipient(RECIPIENT1)
            .recipient(RECIPIENT2)
            .build();

        assertThat(dlp.match(mail)).isEmpty();
    }

    @Test
    void matchSenderShouldReturnRecipientsWhenEnvelopSenderMatches() throws MessagingException {
        Dlp dlp = new Dlp(
            asRulesLoaderFor(
                JAMES_APACHE_ORG_DOMAIN,
                DlpDomainRules.of(DlpDomainRule.factory()
                    .senderRule(Id.of("match sender"), Pattern.compile(ANY_AT_JAMES.asString())))));

        FakeMail mail = FakeMail.builder().sender(ANY_AT_JAMES).recipient(RECIPIENT1).build();

        assertThat(dlp.match(mail)).contains(RECIPIENT1);
    }

    @Test
    void matchSenderShouldReturnRecipientsWhenFromHeaderMatches() throws MessagingException {
        Dlp dlp = new Dlp(
            asRulesLoaderFor(
                JAMES_APACHE_ORG_DOMAIN,
                DlpDomainRules.of(DlpDomainRule.factory()
                    .senderRule(Id.of("match sender"), Pattern.compile(ANY_AT_JAMES.asString())))));

        FakeMail mail = FakeMail
            .builder()
            .sender(OTHER_AT_JAMES)
            .recipient(RECIPIENT1)
            .mimeMessage(MimeMessageBuilder
                .mimeMessageBuilder()
                .addFrom(ANY_AT_JAMES.toInternetAddress()))
            .build();

        assertThat(dlp.match(mail)).contains(RECIPIENT1);
    }

    @Test
    void matchShouldReturnRecipientsWhenEnvelopRecipientsMatches() throws MessagingException {
        Dlp dlp = new Dlp(
            asRulesLoaderFor(
                JAMES_APACHE_ORG_DOMAIN,
                DlpDomainRules.of(DlpDomainRule.factory()
                    .recipientRule(Id.of("match recipient"), Pattern.compile(RECIPIENT1.asString())))));

        FakeMail mail = FakeMail.builder()
            .sender(ANY_AT_JAMES)
            .recipient(RECIPIENT1)
            .recipient(RECIPIENT2)
            .build();

        assertThat(dlp.match(mail)).contains(RECIPIENT1, RECIPIENT2);
    }

    @Test
    void matchShouldReturnRecipientsWhenToHeaderMatches() throws MessagingException {
        Dlp dlp = new Dlp(
            asRulesLoaderFor(
                JAMES_APACHE_ORG_DOMAIN,
                DlpDomainRules.of(DlpDomainRule.factory()
                    .recipientRule(Id.of("match recipient"), Pattern.compile(RECIPIENT2.asString())))));

        FakeMail mail = FakeMail
            .builder()
            .sender(OTHER_AT_JAMES)
            .recipient(RECIPIENT1)
            .mimeMessage(MimeMessageBuilder
                .mimeMessageBuilder()
                .addToRecipient(RECIPIENT2.toInternetAddress()))
            .build();


        assertThat(dlp.match(mail)).contains(RECIPIENT1);
    }

    @Test
    void matchShouldReturnRecipientsWhenCcHeaderMatches() throws MessagingException {
        Dlp dlp = new Dlp(
            asRulesLoaderFor(
                JAMES_APACHE_ORG_DOMAIN,
                DlpDomainRules.of(DlpDomainRule.factory()
                    .recipientRule(Id.of("match recipient"), Pattern.compile(RECIPIENT2.asString())))));

        FakeMail mail = FakeMail
            .builder()
            .sender(OTHER_AT_JAMES)
            .recipient(RECIPIENT1)
            .mimeMessage(MimeMessageBuilder
                .mimeMessageBuilder()
                .addCcRecipient(RECIPIENT2.toInternetAddress()))
            .build();


        assertThat(dlp.match(mail)).contains(RECIPIENT1);
    }

    @Test
    void matchShouldReturnRecipientsWhenBccHeaderMatches() throws MessagingException {
        Dlp dlp = new Dlp(
            asRulesLoaderFor(
                JAMES_APACHE_ORG_DOMAIN,
                DlpDomainRules.of(DlpDomainRule.factory()
                    .recipientRule(Id.of("match recipient"), Pattern.compile(RECIPIENT2.asString())))));

        FakeMail mail = FakeMail
            .builder()
            .sender(OTHER_AT_JAMES)
            .recipient(RECIPIENT1)
            .mimeMessage(MimeMessageBuilder
                .mimeMessageBuilder()
                .addBccRecipient(RECIPIENT2.toInternetAddress()))
            .build();


        assertThat(dlp.match(mail)).contains(RECIPIENT1);
    }

    @Test
    void matchShouldReturnRecipientsWhenSubjectHeaderMatches() throws MessagingException {
        Dlp dlp = new Dlp(
            asRulesLoaderFor(
                JAMES_APACHE_ORG_DOMAIN,
                DlpDomainRules.of(DlpDomainRule.factory()
                    .contentRule(Id.of("match subject"), Pattern.compile("pony")))));

        FakeMail mail = FakeMail
            .builder()
            .sender(OTHER_AT_JAMES)
            .recipient(RECIPIENT1)
            .mimeMessage(MimeMessageBuilder
                .mimeMessageBuilder()
                .setSubject("I just bought a pony"))
            .build();


        assertThat(dlp.match(mail)).contains(RECIPIENT1);
    }

    @Test
    void matchShouldReturnRecipientsWhenMessageBodyMatches() throws MessagingException {
        Dlp dlp = new Dlp(
            asRulesLoaderFor(
                JAMES_APACHE_ORG_DOMAIN,
                DlpDomainRules.of(DlpDomainRule.factory()
                    .contentRule(Id.of("match content"), Pattern.compile("horse")))));

        FakeMail mail = FakeMail
            .builder()
            .sender(OTHER_AT_JAMES)
            .recipient(RECIPIENT1)
            .mimeMessage(MimeMessageBuilder
                .mimeMessageBuilder()
                .setSubject("I just bought a pony")
                .setText("It's actually a horse, not a pony"))
            .build();


        assertThat(dlp.match(mail)).contains(RECIPIENT1);
    }

    //TODO: We lack some test about more complex messages

    @Test
    void matchShouldAttachMatchingRuleNameToMail() throws MessagingException {
        Dlp dlp = new Dlp(
            asRulesLoaderFor(
                JAMES_APACHE_ORG_DOMAIN,
                DlpDomainRules.of(
                    DlpDomainRule.factory()
                        .recipientRule(Id.of("should not match recipient"), Pattern.compile(RECIPIENT3.asString())),
                    DlpDomainRule.factory()
                        .senderRule(Id.of("should match sender"), Pattern.compile(JAMES_APACHE_ORG)))));

        FakeMail mail = FakeMail.builder()
            .sender(ANY_AT_JAMES)
            .recipient(RECIPIENT1)
            .recipient(RECIPIENT2)
            .build();

        dlp.match(mail);

        assertThat(mail.getAttribute("DlpMatchedRule")).isEqualTo("should match sender");
    }

}