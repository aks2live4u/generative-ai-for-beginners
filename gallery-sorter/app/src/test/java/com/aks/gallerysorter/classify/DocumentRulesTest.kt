package com.aks.gallerysorter.classify

import com.aks.gallerysorter.core.Category
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DocumentRulesTest {
    private val mrz1 = "P<INDDOE<<JOHN<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
    private val mrz2 = "L898902C36IND7408122M1204159<<<<<<<<<<<<<<06"

    @Test fun luhn() {
        assertThat(luhnValid("4111111111111111")).isTrue()
        assertThat(luhnValid("4111111111111112")).isFalse()
        assertThat(luhnValid("")).isFalse()
        assertThat(luhnValid("41a1")).isFalse()
    }

    @Test fun validCardIsBankingStrong() {
        val r = documentCategory("4111 1111 1111 1111")!!
        assertThat(r.category).isEqualTo(Category.BANKING)
        assertThat(r.confidence).isEqualTo(0.9f)
    }

    @Test fun cardWithHyphensAndLineBreaks() {
        assertThat(documentCategory("4111-1111-1111-1111")?.category).isEqualTo(Category.BANKING)
        assertThat(documentCategory("4111 1111\n  1111   1111")?.category).isEqualTo(Category.BANKING)
    }

    @Test fun invalidLuhnAloneIsNotBanking() {
        assertThat(documentCategory("4111 1111 1111 1112")?.category).isNotEqualTo(Category.BANKING)
    }

    @Test fun ifsc() {
        assertThat(documentCategory("IFSC: HDFC0001234")?.category).isEqualTo(Category.BANKING)
        assertThat(documentCategory("hdfc0001234")?.category).isEqualTo(Category.BANKING)
    }

    @Test fun upiHandle() {
        assertThat(documentCategory("Paid to rahul@okaxis via UPI")?.category).isEqualTo(Category.BANKING)
        assertThat(documentCategory("send to rahul.k@ybl please")?.category).isEqualTo(Category.BANKING)
    }

    @Test fun plainEmailIsNotUpi() {
        assertThat(documentCategory("write to name@gmail.com")).isNull()
        assertThat(documentCategory("name@gmail.com.")).isNull()
    }

    @Test fun statementKeywords() {
        assertThat(documentCategory("Account Statement\nOpening ...\nClosing Balance 1,200")?.category)
            .isEqualTo(Category.BANKING)
    }

    @Test fun singleBankingKeywordIsReviewBand() {
        val r = documentCategory("Credited")!!
        assertThat(r.category).isEqualTo(Category.BANKING)
        assertThat(r.confidence).isEqualTo(0.6f)
    }

    @Test fun passportMrz() {
        val r = documentCategory("$mrz1\n$mrz2")!!
        assertThat(r.category).isEqualTo(Category.IDS)
        assertThat(r.confidence).isAtLeast(0.9f)
    }

    @Test fun singleMrzLineIsNotEnough() {
        assertThat(documentCategory(mrz1)).isNull()
    }

    @Test fun panNeedsKeyword() {
        val r = documentCategory("Income Tax Department\nABCDE1234F")!!
        assertThat(r.category).isEqualTo(Category.IDS)
        assertThat(r.confidence).isEqualTo(0.95f)
        assertThat(documentCategory("ABCDE1234F")).isNull()
    }

    @Test fun aadhaarWithKeyword() {
        assertThat(documentCategory("Aadhaar 1234 5678 9012")?.category).isEqualTo(Category.IDS)
        assertThat(documentCategory("Government of India\n1234-5678-9012")?.category).isEqualTo(Category.IDS)
    }

    @Test fun twelveDigitsWithoutKeywordIsNotIds() {
        assertThat(documentCategory("call 1234 5678 9012")).isNull()
        assertThat(documentCategory("9876543210 12")?.category).isNotEqualTo(Category.IDS)
    }

    @Test fun drivingLicence() {
        val r = documentCategory("Driving Licence\nDL No MH12 20110012345")!!
        assertThat(r.category).isEqualTo(Category.IDS)
        assertThat(r.confidence).isEqualTo(0.95f)
    }

    @Test fun idsBeatsBanking() {
        assertThat(documentCategory("Aadhaar 1234 5678 9012 and 4111 1111 1111 1111")?.category)
            .isEqualTo(Category.IDS)
    }

    @Test fun longGeneralTextIsOtherDocs() {
        val text = (1..70).joinToString(" ") { "word$it".replace(Regex("\\d"), "x") + "yz" }
        val r = documentCategory(text)!!
        assertThat(r.category).isEqualTo(Category.OTHER_DOCS)
        assertThat(r.confidence).isAtLeast(0.6f)
    }

    @Test fun shortTextIsNull() {
        assertThat(documentCategory("lol")).isNull()
        assertThat(documentCategory("")).isNull()
        assertThat(documentCategory("a few plain words only")).isNull()
    }
}
