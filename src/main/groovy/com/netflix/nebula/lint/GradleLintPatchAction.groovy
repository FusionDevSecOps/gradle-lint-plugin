import static FileMode.Symlink
    private static determinePatchType(List<GradleLintFix> patchFixes) {
        if (patchFixes.size() == 1 && patchFixes.get(0) instanceof GradleLintDeleteFile)
        else if (patchFixes.size() == 1 && patchFixes.get(0) instanceof GradleLintCreateFile) {
    private static readFileOrSymlink(File file, FileMode mode) {
        return mode == Symlink ? [readSymbolicLink(file.toPath()).toString()] : file.readLines()
    private static diffHintsWithMargin(String relativePath, PatchType patchType, FileMode fileMode) {
        def headers = ["diff --git a/$relativePath b/$relativePath"]
                headers += "new file mode ${fileMode.mode}"
                headers += "deleted file mode ${fileMode.mode}"
            def (individualFixes, combinedFixes) = fileFixes.split { it instanceof RequiresOwnPatchset }
            individualFixes.each {
                patchSets.add([it] as List<GradleLintFix>)
            if(combinedFixes)
                patchSets.add((combinedFixes as List<GradleLintFix>).sort { it.from() })
        }

        for(patchSet in patchSets) {
            boolean overlap = true
            while(overlap) {
                patchSet.eachWithIndex { fix, i ->
                    if (i < patchSet.size() - 1) {
                        def next = patchSet[i + 1]
                        def involvesAnInsertion = fix.from() > fix.to() || next.from() > next.to()

                        if ((fix.from() <= next.from() && fix.to() >= next.to() ||
                                next.from() <= fix.from() && next.to() >= fix.to()) &&
                                !involvesAnInsertion) {
                            next.markAsUnfixed(UnfixedViolationReason.OverlappingPatch)
                        }
                    }
                }
                overlap = patchSet.retainAll { it.reasonForNotFixing == null }
            }
            def fileMode = patchType == Create ? (patchFixes[0] as GradleLintCreateFile).fileMode : FileMode.fromFile(file)
                    readFileOrSymlink(file, fileMode).size() == 0) : true
            def newlineAtEndOfOriginal = emptyFile ? false : fileMode != Symlink && file.text[-1] == '\n'
            if (!emptyFile) lines += readFileOrSymlink(file, fileMode)
                    def beforeContext
                    if(j == 0) {
                        def firstLine = Math.max(fix.from() - 3, 1)
                        beforeContext = lines.subList(firstLine, fix.from())
                    }
                    else {
                        try {
                            beforeContext = lines.subList(patchFixes[j - 1].to() + 1, fix.from())
                        } catch(IllegalArgumentException e) {
                            throw new RuntimeException("tried to overlay patches with ranges [${patchFixes[j-1].from()}, ${patchFixes[j-1].to()}], [${fix.from()}, ${fix.to()}]", e)
                        }
                    }
                    beforeContext = beforeContext
                    if(j == 0) {
                        StringUtils.isNotBlank(line) ? '+' + line : null
                if (fix.to() < lines.size() - 1 && lastFix) {
                    def lastLineOfContext = Math.min(fix.to() + 3 + 1, lines.size())
                    if (lastLineOfContext == lines.size() && !newlineAtEndOfOriginal) {
            def relativePath = project.rootDir.toPath().relativize(file.toPath()).toString()
                ${diffHintsWithMargin(relativePath, patchType, fileMode)}
                |--- ${patchType == Create ? '/dev/null' : 'a/' + relativePath}
                |+++ ${patchType == Delete ? '/dev/null' : 'b/' + relativePath}
}