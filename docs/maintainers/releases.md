# Releases and public launch

## Source milestones

The first source tag is v0.3.3, corresponding to app version 0.3.3-opacity and versionCode 42. Keep existing tags immutable. Repository documentation/CI cleanup can proceed on main without changing the installed application version.

For an application release, update versionName/versionCode, the changelog, relevant compatibility evidence and dependency notices together. Identify the exact source tag and submodule revisions used for each APK.

## Validation and artifacts

Run the relevant repository, Android and browser checks documented in [testing](../development/testing.md). Record CI results only after the host actually runs the workflow. Hardware-sensitive changes need explicit device acceptance; do not automatically install or restart a household TV.

Verify APK signatures and SHA-256 before distribution. Debug signatures are host-specific. A stable release-signing identity and its backup/rotation process must be chosen before promising upgrade compatibility across published APKs.

Keep a source tag available with submodule revisions, patches, build instructions, license texts and notices. A Git source ZIP alone does not include submodule contents.

## Public launch

Public source is hosted at [czp-cloud/tv-airplay-float](https://github.com/czp-cloud/tv-airplay-float). Private vulnerability reporting is enabled through [GitHub Security Advisories](https://github.com/czp-cloud/tv-airplay-float/security/advisories/new). Preserve existing upstream and derivative history, including the immutable v0.3.3 milestone.

For future releases or additional public mirrors:

1. Choose the public host/owner/name and set its description, topics and default branch.
2. Run and inspect CI on that host; do not add a passing badge before a real run.
3. Configure a reachable private vulnerability-reporting channel and update SECURITY.md.
4. Review the full Git history, not just main's current files, for personal data and credentials.
5. Decide whether to publish the existing history or create a reviewed public export. The first private milestone's history contains legacy local network/setup references; current-file cleanup does not erase historical objects.
6. If rewriting history or replacing tags is needed, obtain explicit authorization and retain a private backup. Do not force-push the private repository as incidental cleanup.
7. Publish a source-only milestone or a deliberately signed APK, accurately labeled with its verification limits.

Check the destination before every push. Keep the original upstream remote separate from project-owned hosts; never push this derivative to the upstream author's repository. Publishing a new mirror does not authorize changing another host's visibility or rewriting its history.

## Upstream updates

Keep upstream as a separate remote. Review and merge deliberately, preserving the derivative's application ID, overlay lifecycle and audio policy. Recheck native patches and dependencies against their new pins; rerun applicable tests and update the dependency inventory.
