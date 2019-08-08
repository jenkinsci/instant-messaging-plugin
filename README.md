Jenkins Instant-Messaging plugin [![Build Status](https://buildhive.cloudbees.com/job/jenkinsci/job/instant-messaging-plugin/badge/icon)](https://buildhive.cloudbees.com/job/jenkinsci/job/instant-messaging-plugin/)
================================

This plugin provides abstract support for instant-messaging notifications to Jenkins.
It's implemented by various concrete plugins like the Jabber or the IRC plugin.

For more information, visit the wiki page:
<https://wiki.jenkins-ci.org/display/JENKINS/Instant+Messaging+Plugin>

Regex support in bot commands
-----------------------------

The `currentlyBuilding` (`cb`) and `queue` (`q`) commands now support an
optional argument to apply regular expression filters to the lines they
would output, and count and report hits compared to the full job lists.

The key argument for this is `~` and the rest of the argument string is
assumed to be the regular expression (note however that whitespace between
tokens of the argument string is converted during argument processing).

Beside ordinarily matching what is *present* in a line, this can be used
with "negative lookups" for matching lines that *do not contain* certain
patterns. Certain know-how applies to such expressions:

* To negative-match the whole string from start, use a caret, e.g.:

````
cb ~ ^(?!master)
````
to skip jobs running on nodes whose name starts with (or equals) `master`.
Without the caret it did not work.

* For matching inside a job name, the `.*` must be also inside the
lookahead pattern:

````
cb ~ ^(?!.*rescan)
````

would list only jobs whose IM report line does NOT include `rescan`.
