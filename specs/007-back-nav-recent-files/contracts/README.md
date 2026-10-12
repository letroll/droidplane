# Contracts

This feature has no external contracts (public APIs, CLI schemas, web endpoints, grammars, or parser interfaces).

It is an internal Android application feature affecting only:
- UI navigation behavior (back button handling)
- Dialog presentation (Recent Files dialog)
- Toolbar menu items

All interactions are internal to the `:app` module via Kotlin function calls and Compose UI events.