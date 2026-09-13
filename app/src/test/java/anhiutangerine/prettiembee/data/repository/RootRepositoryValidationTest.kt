package anhiutangerine.prettiembee.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootRepositoryValidationTest {
    @Test fun packageValidationRejectsShellAndPathSyntax() {
        assertTrue(RootRepository.isValidPackageName("com.mbmobile"))
        assertTrue(RootRepository.isValidPackageName("com.example.mb_clone"))
        listOf("", "mbmobile", "../../data", "com.mb;id", "com.mb mobile", "com.mb\nmobile")
            .forEach { assertFalse(it, RootRepository.isValidPackageName(it)) }
    }

    @Test fun uuidValidationIsStrict() {
        assertTrue(RootRepository.isValidUuid("aa3cb89a-8325-41b4-b59b-dfaea086cf80"))
        listOf("", "../uuid", "aa3cb89a-8325-41b4-b59b-dfaea086cf80;id", "not-a-uuid")
            .forEach { assertFalse(it, RootRepository.isValidUuid(it)) }
    }
}
