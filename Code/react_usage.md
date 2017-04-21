Guidelines for effective React usage
====================================

## Px, Reusability, cache invalidation

This gets a bit difficult which is justified because as they say:
there are only two hard things in Computer Science: cache invalidation and naming things.
`Px` covers both by having a shit name and a sole purpose that is caching!
Here are some rules that explain how to use `Px` with React components without
incurring subtle cache invalidation bugs.

* DON'T pass `Px` (using *props*) to a component that uses `shouldComponentUpdate`.
  This will create a bug when the `Px` updates but the UI doesn't.

  If you really want `Px[A]` inside and outside of the component *and* you want reusability,
  pass the reusable `A` through the props and re-wrap it with `Px` on the inside.

* DON'T use `Px` in *state* of a component.
  That would be madness... It's really not made for that use case.
  At the very least, doing so creates a hidden dependency which will not be checkable in `shouldComponentUpdate`.
  This will create a bug when the `Px` updates but the UI doesn't.

* Sometimes, it's a great fitting strategy to use
  static props which are specified once and don't need reusability,
  and dynamic props which are specified thereafter and have reusability.
  Doing so means you create a new component each time the static props change but the point is that you shouldn't change them.

  Example:

  ```scala
  object Example {
    final case class StaticProps(/* ... */)

    final case class DynamicProps(/* ... */)
    implicit val reusability = Reusability.caseClass[DynamicProps]

    final class Backend(sp: StaticProps, bs: BackendScope[DynamicProps, Unit]) {
      // ...
    }

    def prepare(sp: StaticProps): ScalaComponent[DynamicProps, Unit, Backend, CtorType.Props] =
      ScalaComponent.builder[DynamicProps]("Example")
        .backend(new Backend(sp, _))
        .renderBackend
        .build
  }
  ```

* Should you use `Px[A]` in static props or `A` in dynamic props?

  If your component *does* use `shouldComponentUpdate`:
    * If you will not make any impure calls to the `Px`, pass it in through static props.
      For example,
        * you never call `Px#value()` to get the `A`
        * you never call `Px#toCallback()#runNow()` to get the `A`
      Typically, this means you are restricted to the following:
        * `Px#toCallback` (yes, it's pure)
        * `Px#map` and `Px#flatMap`
        * pass the `Px` around to something else
    * Otherwise, pass the `A` through dynamic props.

  If your component does *not* use `shouldComponentUpdate`:
    * If need `Px[A]` in your component, pass in the `Px[A]` through static props.
    * Otherwise, pass the `A` through dynamic props.




## Creating Features

I (@japgolly) have found in my own app that there are a number of cases in my main work project
where it is desirable to create "features" that:

* represent a single, logical functionality or concept, usually orthogonal to components
* are reusable in that they can be applied to multiple components
* compose with any other features
* may require state to be maintained

After lots of use and failed experiments, this is what I've settled on as the best strategy:

Arbitrary composition of features *has* to be manual.
Especially where VDOM is concerned, there can be way too many hooks and when multiple features affect the same hooks
there's no automatically correct way to compose them. Sometimes even when multiple features affect different hooks
additional logic is still required (eg. before feature #2 `onClick` does it's thing it now has to check a condition in
feature #1).
This is similar to the problems with monad composition and monad transformers.
For common compositions create wrappers (eg `Feature1And2`) to house all the specific composition logic.

Prefer exposing all hooks and functions to feature users and reqiure them to wire it all up themselves.
It sounds stupid and tedious but:
1) it's required for composition (see above)
2) I've learned you can never fully anticipate the shapes and structures of external components' and their VDOM.
   if you try to create a function that asks component's for their details then wires up the hooks for them, you'll
   only end up with huge, obscure lambdas and you'll keep finding with each new usage that it doesn't work and you
   need to modify it all.

I've found that a very useful strategy

Feature
- Stateless - *asks* for state to be provided by the caller when required
- Logic
- Sometimes has (pure) access to feature state.
  - eg. R/W access with `StateAccessPure[S]`
  - eg. R access with `CallbackTo[S]`
  - eg. W access with `S => Callback`
- Usage style #1
  - Created by components directly as needed
  - Not reusable
- Usage style #2
  - Created once, passed down to children components
  - Reusable by reference
  - All logic must be pure (e.g. `.setState` is fine as long as you return a `Callback`; calling `.runNow()` on a `Callback` on the other hand is impure and a big no-no with reusability)


State
- An immutable value
- Reusable by contents

Dimensions
