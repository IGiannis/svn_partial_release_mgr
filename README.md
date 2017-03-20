# Jenkins plugin - SVN Partial Release Manager
This plugin provides the option to create a build for a partial release

# Description
The following scenario is very common and this plugin tries to provide a solution for it. 
Case
-	The development team releases into the production a major version commits all the code and creates a tag to SVN to represent the release 
-	The next release is agreed to have a number of issues and the developers start committing the issue implementation code.
-	Release date arrives and for whatever reason (not tested thoroughly, business implications) the client decides that from the 10 original release issues it will move to production only the 2 of them.
-	Or the client needs urgently the 1 of them to be moved now to production and the rest on release date.

Solution
- The plugin will do the following to provide a partial release option
- Allow the user to choose which issues to release (actually the SVN revisions that have been committed for these issues provided that all commits have the issue number into the commit message) 
- Checkout the source of the latest stable version tag (configured into the job by the user)
- Get the source of the chosen revisions.
- Combine the above two and build the final source as a normal maven job to provide the final deployment war
