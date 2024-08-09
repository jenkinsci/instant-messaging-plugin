Jenkins Instant-Messaging plugin
================================

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/instant-messaging.svg)](https://plugins.jenkins.io/instant-messaging)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/instant-messaging-plugin.svg?label=release)](https://github.com/jenkinsci/instant-messaging-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/instant-messaging.svg?color=blue)](https://plugins.jenkins.io/instant-messaging)
[![Build Status](https://ci.jenkins.io/job/Plugins/job/instant-messaging-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/instant-messaging-plugin/job/master/)
[![GitHub license](https://img.shields.io/github/license/jenkinsci/instant-messaging-plugin.svg)](https://github.com/jenkinsci/instant-messaging-plugin/blob/master/LICENSE.txt)
[![Maintenance](https://img.shields.io/maintenance/yes/2024.svg)](https://github.com/jenkinsci/instant-messaging-plugin)

Introduction
------------

This plugin provides abstract support for instant-messaging notifications
to Jenkins, including a common bot-driven command and query interface
and the core for pipeline steps integration code.

Practical support for particular IM protocols is implemented by various
concrete plugins like the
[Jabber/XMPP](https://github.com/jenkinsci/jabber-plugin) or the
[IRC](https://github.com/jenkinsci/ircbot-plugin) plugin, allowing users
to have the same interactions with their Jenkins server whatever messenger
they use.

Note that there are some other IM protocol plugins that chose to implement
everything from scratch, so they do not use `instant-messaging-plugin` as
a dependency and do not enjoy its common features.

There are also plugins which add commands available via instant messaging
such as [Log Command](https://github.com/jenkinsci/log-command-plugin).

For more information, visit the plugin information page:
<https://plugins.jenkins.io/instant-messaging/>

The practical suggestions for freestyle or pipeline job setup may be seen in
<https://plugins.jenkins.io/ircbot/> and <https://plugins.jenkins.io/jabber/>
pages.

Regex support in bot commands
-----------------------------

The `currentlyBuilding` (`cb`) and `queue` (`q`) commands now support an
optional argument to apply regular expression filters to the lines they
would output, and count and report hits compared to the full job lists.

The key argument for this is `~` and the rest of the argument string is
assumed to be the regular expression (note however that whitespace between
tokens of the argument string is converted during argument processing;
for clarity it would be preferable to pass a single-token argument with
`\s+` in positions where whitespace matches are expected).

Beside ordinarily matching what is *present* in a line, this can be used
with "negative lookups" for matching lines that *do not contain* certain
patterns. Certain know-how applies to such expressions:

* To negative-match the whole string from start, use a caret, e.g.:

````
developer> cb ~ ^(?!master)
````
to skip jobs running on nodes whose name starts with (or equals) `master`.
Without the caret it did not work.

* For matching inside a job name, the `.*` must be also inside the
lookahead pattern:

````
developer> cb ~ ^(?!.*rescan)
````

would list only jobs whose IM report line does NOT include `rescan`.

* Similarly, these commands can be used to quickly gauge how many builds
are queued or running (without spelling out each one), with a regex filter
or overall:

````
developer> jenkins: q # ~ mycomponent
jenkins> (notice) Build queue (19 items total, of which 2 items matched the filter)

developer> jenkins: cb #
jenkins> (notice) Currently building (11 items)
````

* The `currentlyBuilding` command can also be asked to provide URLs to the
console of the reported running builds, so you can quickly tap into them,
e.g.:

````
developer> jenkins: cb @ ~ UI
jenkins> (notice) - ui-builder-1#0: UIbuild #25190
  @ https://jenkins.some.org/job/UIbuild/25190/console
  (Elapsed time: 2 min 31 sec, Estimated remaining time: 4 min 35 sec)
````
