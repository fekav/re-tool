# Architecture

## Project Structure
```text


src/main/java/io/fekav
├── platform                        // project-wide, technical layer
│   ├── api                         // REST, CLI
│   ├── infrastructure              // interface adapter
│   └── cqrs                        // marker interfaces for simple CQRS
│
├── <domain>
│   ├── shared                      // domain-wide, cross-slice
│   │   ├── model                   
│   │   ├── events                          
│   │   ├── ports                    
│   │   └── adapters                 
│   │
│   ├── <vertical-slice>            //
│   │   ├── application             // Command, Query, Handler, UseCase
│   │   ├── domain                  // Entities, Value Objects, Domain Services, Policies
│   │   ├── ports                   // optional
│   │   └── adapters                // optional
│   │
│   └── <vertical-slice>
│       └── ...
└── <domain>
    └── ...
```