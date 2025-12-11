/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.exception.AcmeRetryAfterException;
import org.shredzone.acme4j.exception.AcmeServerException;

import lombok.experimental.UtilityClass;

/**
 * Classifies exceptions into transient vs permanent for retry purposes.
 */
@UtilityClass
public class TransientErrorClassifier {

    /**
     * Returns true if the exception is likely transient and worth retrying.
     *
     * @param e exception
     * @return true if transient
     */
    public static boolean isTransient(final Exception e) {
        if (e == null) {
            return false;
        }
        // Network related issues: retry
        if (e instanceof SocketTimeoutException || e instanceof UnknownHostException
            || e instanceof ConnectException || e instanceof SocketException
            || e instanceof IOException) {
            return true;
        }

        // ACME specific retriable conditions
        if (e instanceof AcmeRetryAfterException) {
            return true;
        }
        if (e instanceof AcmeServerException) {
            // Treat ACME server-side errors as transient; specific status access may vary by version.
            return true;
        }

        // Unknown AcmeException without details: treat as transient conservatively
        if (e instanceof AcmeException) {
            return true;
        }

        return false;
    }
}
