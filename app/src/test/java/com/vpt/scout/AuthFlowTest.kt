package com.vpt.scout

import com.vpt.scout.ui.screens.validateSignUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthFlowTest {

    @Test
    fun `unsubscribed sign-in goes to the paywall`() {
        assertEquals(AuthStep.SUBSCRIBE, authStepAfterSignInFailure(NotApprovedException()))
        assertEquals(AuthStep.SIGN_IN, authStepAfterSignInFailure(IllegalStateException("Invalid login credentials")))
        assertEquals(AuthStep.SIGN_IN, authStepAfterSignInFailure(null))
    }

    @Test
    fun `sign-up outcome picks the next screen`() {
        assertEquals(AuthStep.CONFIRM_EMAIL, authStepAfterSignUp(SignUpOutcome.ConfirmEmail("a@b.co")))
        assertEquals(AuthStep.SUBSCRIBE, authStepAfterSignUp(SignUpOutcome.PendingApproval("a@b.co")))
        assertEquals(AuthStep.SIGN_IN, authStepAfterSignUp(SignUpOutcome.SignedIn(SupabaseAuthState(accessToken = "t"))))
    }

    @Test
    fun `recheck messages explain what to do`() {
        assertNull(accessRecheckMessage(AccessCheck.APPROVED))
        assertEquals(true, accessRecheckMessage(AccessCheck.NOT_APPROVED)?.contains("subscription"))
        assertEquals(true, accessRecheckMessage(AccessCheck.UNKNOWN)?.contains("connection"))
    }

    @Test
    fun `validateSignUp checks email, length, and match`() {
        assertEquals("Enter a valid email address.", validateSignUp("not-an-email", "longpassword", "longpassword"))
        assertEquals("Password must be at least 8 characters.", validateSignUp("a@b.co", "short", "short"))
        assertEquals("Passwords don't match.", validateSignUp("a@b.co", "longpassword", "longpassw0rd"))
        assertNull(validateSignUp("  a@b.co ", "longpassword", "longpassword"))
    }
}
