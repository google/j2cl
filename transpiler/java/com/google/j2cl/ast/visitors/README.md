# Contained here are J2CL AST normalization visitors.

## Expectations
- They should be for normalization and structural/interdependent AST changes.
- They should not be for simple data gathering (simple data gathering should
  be generator package visitors that are run as part of template context
  construction).
- When modifying AST, don't copy information, always reference. (For example
  don't synthesize Import nodes from TypeReference data because doing so
  creates the opportunity for data desynchronization if TypeReference content
  is updated.)
