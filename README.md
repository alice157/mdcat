# mdcat
`mdcat` parses, selects, and transforms markdown documents. It is intended to be used as part of a shell-scripted workflow, cron job, or similar.

Selections are done with a custom, CSS-style selection language, and transformation is done by calling out to an external program (usually a babashka script).

# Project status
Still very early stages, although most of the key pieces of functionality are implemented. Currently solves my main motivating usecase of fixing my HRT log, but does not do everything I want it to do.

Notably, it only supports selectors over a very small subset of markdown, although adding more shouldn't be too difficult (see `src/mdcat/markdown.clj`, `src/mdcat/markdown/render.clj`, and `src/mdcat/selector.clj`. I'm thinking about how to structure this code, because that feels like a lot of places to add code, but the concerns otherwise feel separated correctly... feedback welcome!)

!!! MAY DESTROY DATA !!!

This is not yet tested thoroughly enough to say it's safe. Use version control, or don't squash files with shell redirection. I hope to get this to a point where I can say it's probably safe, but this is not there yet.

# Usage
Can be used as a binary, jar, or just with `lein run`. Scripts for builds are available in `scripts/`, although they haven't been tested in a bit as I'm primarily working on Apple Silicon these days.

Options can be listed with `lein run -- -h` (the `--` lets lein know that the remaining args are for the program, not lein ;) ).

`lein run -- <ARGS> file.md`

- `-h` prints help and exits
- `-o` prints the parsed options map on startup, useful for debugging
- `-s SELECTOR` runs the given selector against the file
- `-x COMMAND` calls the command with the selected fragments (as edn), and updates the fragment with the response (also expects edn)
- `-t` outputs as text, instead of edn

Currently, `mdcat` reads from `file.md`, and outputs to standard out. This may be changed in the future to either write to the given file by default, or possibly take input through stdin by default.

# Markdown
## Parsing
Parsing is done with flexmark. The parsed tree is then converted to edn.

```markdown
- foo bar baz
- wibble
  - wobble
```

becomes

```edn
[:md/document
 [:md/bullet-list
  [:md/bullet-list-item
    [:md/paragraph [:md/text "foo bar baz"]]]
  [:md/bullet-list-item
   [:md/paragraph [:md/text "wibble"]]
   [:md/bullet-list
     [:md/bullet-list-item
       [:md/paragraph [:md/text "wobble"]]]]]]]
```

Transformating programs are expected to take and return fragments of these trees as edn.

## Selectors
Selectors are composable, CSS-style selectors that pick out parts of the document by structure. Programmable selectors are being considered, but currently, non-structural selections should be handled by a transforming program that no-ops on non-matches.

The infrastructure is there for more selectors, but the current symbols supported are `paragraph`, `list`, and `item`. `list` and `item` assume bullet lists - numbered lists are not yet implemented. There are 3 modes for selectors - default (no prefix), shallow (prefix with `.` - `.list`), and deep (prefix with `*` - `*list`).

Default will walk the tree until it finds a matching part of the tree, and won't traverse deeper. Shallow will only consider direct children, and deep will select the outer match and continue inwards, walking the entire tree.

On


```edn
foo.md

[:md/document
 [:md/bullet-list
  [:md/bullet-list-item
    [:md/paragraph [:md/text "foo bar baz"]]]
  [:md/bullet-list-item
   [:md/paragraph [:md/text "wibble"]]
   [:md/bullet-list
     [:md/bullet-list-item
       [:md/paragraph [:md/text "wobble"]]]]]]]
```

`lein run  -- -s 'list item' foo.md` returns a vector of matches

```edn
[[:md/bullet-list-item
  [:md/paragraph [:md/text "foo bar baz"]]]

 [:md/bullet-list-item
   [:md/paragraph [:md/text "wibble"]]
     [:md/bullet-list
       [:md/bullet-list-item
       [:md/paragraph [:md/text "wobble"]]]]]]
```

If we wanted to match every item, we could instead make one of the selectors deep:

`lein run --s -s '*list item' foo.md` returns 3 matches:

```edn
[[:md/bullet-list-item [:md/paragraph [:md/text "wobble"]]]

 [:md/bullet-list-item [:md/paragraph [:md/text "foo bar baz"]]]

 [:md/bullet-list-item
  [:md/paragraph [:md/text "wibble"]]
  [:md/bullet-list [:md/bullet-list-item [:md/paragraph [:md/text "wobble"]]]]]]
```

The full space of what happens when transforming deep selectors has not yet been explored or tested, tread with caution.

Selected snippets can be rendered with `-t`, otherwise they will be given as edn.

## Transformations
Transformations are implemented a bit strangely at the moment, and I'm thinking about better ways to do it. Currently, it takes a program that should take edn on STDIN, and replaces the selected fragment with STDOUT. This is intended to be used with babashka scripts, but that isn't a requirement. The command cannot be specified with any arguments yet, hopefully I'll fix that soon. I'm planning on spinning out some of the markdown predicates and specs to a separate library, but that isn't done yet.

If we had a list

```markdown
- 2
  - wibble
- 1
  - wobble
```

and we wanted to reorder it so that `1` is first, we could write a script `reverse-list.clj` (must be marked executable!)

```clojure
#!/usr/bin/env -S bb --prn

(into [(first *input*)] (reverse (rest *input*)))
```

and call it with `lein run -- -s "list" -x reverse-list.clj example-list.md`.

If the output looks good, you can switch to rendering it as text with `-t`, and use shell redirection to write it somewhere.

## Rendering
I had trouble figuring out how to surgically alter flexmark trees and render them again, so I hacked together a quick and dirty renderer. It may change spacing, indents, and the like, but ideally it should write out the same parse tree it was given. This is not well-tested, there are almost certainly bugs. Use at your own risk. I will either swap it for flexmark's renderer or make it more robust at some point.

# Building
For maximum cron-job goodness, `mdcat` is intended to be compilable through GraalVM. Most testing has been done by just running it with `lein run`, because development is happening on Apple Silicon, where the Graal build doesn't work. `script/docker-build --output mdcat` will, in theory, give you a binary called `mdcat` when run on a `x86_64` machine with docker.

# Limitations
- Currently fires up a process for every selected fragment when transforming. This feels... bad, and there should probably be options to enable better control over this (maybe enable sending the entire vector of selections, or send entries one by one over stdin).
- Doesn't have enough unit test coverage, and doesn't have any end to end testing.
- I want to somehow add programmable selectors - maybe include sci, and use that for transforms and selectors?
- Needs implementations for more flexmark nodes.
- Some of the markdown stuff should be spun out into a library for use in babashka or sci scripts.
- Something feels off about the way the code is currently organized, although it has improved over the course of this project. Another shuffling of functions/namespaces/responsibilities may be in order.
- Need to better define how transforms work on deep selectors
- Some markdown shorthand may be helpful - having to specify `[:md/paragraph [:md/text "foo"]]` just because flexmark does it that way may not be necessary.
- Need to re-investigate using flexmark's renderer.
- Another cool usecase could be adding a table of contents. Because the document selector is currently not available, I'm not sure if that's possible.

# Thanks
https://github.com/greglook/cljstyle - build scaffolding for GraalVM
