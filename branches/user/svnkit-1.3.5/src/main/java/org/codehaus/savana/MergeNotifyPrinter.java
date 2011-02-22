package org.codehaus.savana;

import org.tmatesoft.svn.cli.svn.SVNNotifyPrinter;
import org.tmatesoft.svn.cli.svn.SVNCommandEnvironment;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.SVNException;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Extends the standard SVNNotifyPrinter and keeps a list of skipped files and files that
 * likely have changes to subversion properties.
 */
public class MergeNotifyPrinter extends SVNNotifyPrinter {
    private final List<File> _skippedFiles = new ArrayList<File>();
    private final Collection<File> _propertiesChangedFiles = new LinkedHashSet<File>();

    public MergeNotifyPrinter(SVNCommandEnvironment env) {
        super(env);
    }

    public List<File> getSkippedFiles() {
        return _skippedFiles;
    }

    public Collection<File> getPropertiesChangedFiles() {
        return _propertiesChangedFiles;
    }

    @Override
    public void handleEvent(SVNEvent event, double progress) throws SVNException {
        super.handleEvent(event, progress);

        if (event.getAction() == SVNEventAction.SKIP) {
            _skippedFiles.add(event.getFile());
        }
        if (event.getAction() == SVNEventAction.PROPERTY_ADD ||
            event.getAction() == SVNEventAction.PROPERTY_MODIFY ||
            (event.getPropertiesStatus() != SVNStatusType.UNKNOWN &&
             event.getPropertiesStatus() != SVNStatusType.UNCHANGED &&
             event.getPropertiesStatus() != SVNStatusType.INAPPLICABLE)) {
            _propertiesChangedFiles.add(event.getFile());
        }
    }
}
