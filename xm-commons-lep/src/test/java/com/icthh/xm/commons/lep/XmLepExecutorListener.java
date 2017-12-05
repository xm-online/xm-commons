package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepExecutorEvent;
import com.icthh.xm.lep.api.LepExecutorEvent.AfterResourceExecutionEvent;
import com.icthh.xm.lep.api.LepExecutorEvent.BeforeResourceExecutionEvent;
import com.icthh.xm.lep.api.LepExecutorListener;

/**
 * The {@link XmLepExecutorListener} class.
 */

// NOTE: unused yet listener
@Deprecated
public class XmLepExecutorListener implements LepExecutorListener {

    private Holder<SecurityManager> previousSecurityManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(LepExecutorEvent event) {
        if (event instanceof BeforeResourceExecutionEvent) {
            onBeforeResourceExecutionEvent(BeforeResourceExecutionEvent.class.cast(event));
        } else if (event instanceof AfterResourceExecutionEvent) {
            onAfterResourceExecutionEvent(AfterResourceExecutionEvent.class.cast(event));
        }
    }

    private void onBeforeResourceExecutionEvent(BeforeResourceExecutionEvent event) {
        // FIXME add LEP script sand-boxing ...
        // Leads to NoClassDefFoundError in spring-boot-loader environment, because of check:
        // sun.misc.URLClassPath.JarLoader#checkJar
        //
        // System.getSecurityManager() != null
        // && !URLClassPath.DISABLE_JAR_CHECKING
        // && !zipAccess.startsWithLocHeader(jarFile)
        //
        // Where jarFile is a org.springframework.boot.loader.jar.JarFile
        // and in java.util.zip.ZipFile
        // this.locsig = startsWithLOC(jzfile) == false;
        //
        // @see java.util.zip.ZipFile:
        // static {
        //     sun.misc.SharedSecrets.setJavaUtilZipFileAccess(
        //         new sun.misc.JavaUtilZipFileAccess() {
        //             public boolean startsWithLocHeader(ZipFile zip) {
        //                 return zip.startsWithLocHeader();
        //             }
        //         }
        //     );
        // }
        //
        // Can try this code:
        // sun.misc.SharedSecrets.setJavaUtilZipFileAccess(zip -> {
        //     if (zip instanceof org.springframework.boot.loader.jar.JarFile) {
        //         return true;
        //     } else {
        //       return old.startsWithLocHeader(zip);
        //     }
        // }

        initScriptSecurityManager();
    }

    private void onAfterResourceExecutionEvent(AfterResourceExecutionEvent event) {
        restoreAppSecurityManager();
    }

    private void initScriptSecurityManager() {
        // save current security manager
        this.previousSecurityManager = Holder.ofNullable(System.getSecurityManager());

        // prevent System.exit & Runtime.exec in scripts
        System.setSecurityManager(new XmLepResourceSecurityManager(previousSecurityManager.orElse(null)));
    }

    private void restoreAppSecurityManager() {
        // restore security manager
        if (previousSecurityManager != null) {
            System.setSecurityManager(previousSecurityManager.orElse(null));
            this.previousSecurityManager = null;
        }
    }


}
