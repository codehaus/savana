package org.codehaus.savana.scripts;

import org.tmatesoft.svn.cli.AbstractSVNOption;

/**
 */
public class SAVOption extends AbstractSVNOption {

    public static final SAVOption CHANGE_ROOT = new SAVOption("changeRoot", "C");

    public static final SAVOption PROJECT_NAME = new SAVOption("projectName", false);
    public static final SAVOption TRUNK_PATH = new SAVOption("trunkPath", false);
    public static final SAVOption RELEASE_BRANCHES_PATH = new SAVOption("releaseBranchesPath", false);
    public static final SAVOption USER_BRANCHES_PATH = new SAVOption("userBranchesPath", false);
    public static final SAVOption VERSIONED_SYMLINKS_SUPPORTED = new SAVOption("versionedSymlinksSupported");

    private SAVOption(String name) {
        this(name, null, true);
    }

    private SAVOption(String name, boolean unary) {
        this(name, null, unary);
    }

    private SAVOption(String name, String alias) {
        this(name, alias, true);
    }

    private SAVOption(String name, String alias, boolean unary) {
        super(name, alias, unary);
    }

    protected String getResourceBundleName() {
        return "org.codehaus.savana.scripts.options";
    }
}
