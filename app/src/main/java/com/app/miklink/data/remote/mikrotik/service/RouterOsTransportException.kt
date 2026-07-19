/*
 * Purpose: Transport exception carrying a typed RouterOsError for classification at the boundary.
 * Inputs: TestError.RouterOsError produced by RouterOsResponseDecoder.
 * Outputs: Exception type used by MikroTikCallExecutor/transport classification.
 * Notes: Keeps the typed error available to the runner without free-form message parsing.
 */
package com.app.miklink.data.remote.mikrotik.service

import com.app.miklink.core.domain.test.model.TestError

class RouterOsTransportException(val error: TestError.RouterOsError) :
    Exception(error.message)
