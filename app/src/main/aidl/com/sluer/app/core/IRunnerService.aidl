// Binder interface between the Sluer app process and its Shizuku-powered
// user service (which runs with shell/root identity).
//
// Method IDs are explicit on purpose: the Shizuku server calls destroy()
// with the reserved transaction code 16777114 when it tears the user
// service down.
package com.sluer.app.core;

interface IRunnerService {
    void destroy() = 16777114;

    String exec(String command) = 1;

    int getUid() = 2;
}
