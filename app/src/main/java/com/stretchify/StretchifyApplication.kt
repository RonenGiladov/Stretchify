package com.stretchify

import android.app.Application
import com.stretchify.data.SampleRoutineProvider
import com.stretchify.session.SessionController

class StretchifyApplication : Application()
{
    val sessionController: SessionController by lazy {
        SessionController(this, SampleRoutineProvider.routines.first())
    }
}
