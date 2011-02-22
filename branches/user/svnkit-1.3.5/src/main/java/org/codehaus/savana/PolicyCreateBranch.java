package org.codehaus.savana;

import org.apache.commons.lang.BooleanUtils;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

import java.util.Properties;

/**
 * Validates the arguments in create branch commands.
 *
 * Currently this checks if property createbranch.toplevel_must_be_explicit is set to 'true',
 * in which case it will enforce that either '--topLevel' or the path at which to root a subbranch
 * must be provided as argument to create branch commands
 */
public class PolicyCreateBranch {

    private final Properties _properties;

    private static final String TOP_LEVEL_MISSING_ERROR
            = "Top level branch creation requires argument '--topLevel' be present.\n"
            + "   Otherwise, to create a subbranch, the subbranch root must be provided.";

    public PolicyCreateBranch(Properties properties) {
        _properties = properties;
    }

    public void validateCreateBranch(boolean createSubBranch, boolean explicitTopLevel) throws SVNException {
        if (!createSubBranch && !explicitTopLevel
                && BooleanUtils.toBoolean(_properties.getProperty("createbranch.toplevel_must_be_explicit"))) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_INSUFFICIENT_ARGS,
                                                         TOP_LEVEL_MISSING_ERROR),
                                  SVNLogType.CLIENT);
        }
    }
}
