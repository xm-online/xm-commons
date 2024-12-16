package com.icthh.xm.commons.domain.comparator;

import com.icthh.xm.commons.domain.spec.FunctionSpec;
import org.springframework.util.AntPathMatcher;

import java.util.Comparator;

public class FunctionSpecPathComparator implements Comparator<FunctionSpec> {

    private final AntPathMatcher matcher = new AntPathMatcher();

    public static FunctionSpecPathComparator of() {
        return new FunctionSpecPathComparator();
    }

    @Override
    public int compare(final FunctionSpec fs1, final FunctionSpec fs2) {
        String path1 = fs1.getPath();
        String path2 = fs2.getPath();
        if (path1 != null && path2 == null) {
            return 1;
        } else if (path1 == null && path2 != null) {
            return -1;
        } else if (path1 == null) {
            return 0;
        } else if (matcher.match(path1, path2)) {
            return 1;
        } else if (matcher.match(path2, path1)) {
            return -1;
        } else {
            return path1.compareTo(path2);
        }
    }
}

