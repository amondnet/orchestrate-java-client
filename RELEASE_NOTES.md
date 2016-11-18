- 0.12.0: 2015-11-06
 * Add support for cross-collection search
 * Allow graph relationships to appear in search results
 * (breaking) Rename "Relation" class to "Relationship", consistent with REST API naming conventions
 * (breaking) Rename "Relation.kind" property to "Relationship.relation", consistent with REST API naming conventions
 * (breaking) Graph "Relationship" class is now a subclass of KvObject
 * (breaking) Search by "kinds" now accepts ItemKind enum arguments, instead of string values. 
 * Parse path.reftime into the KvMetadata (and all descendants)
- 0.11.1: 2015-08-28
 * Add support for JSON properties to graph relations
 * Add support for conditional PUT headers (if-match & if-none-match) to graph relations
 * Add support for prev/next in search queries.
- 0.11.0: 2015-07-01
 * Add support for endKey and beforeKey in kv list
 * GZip request entity bodies
 * Request gzipped responses
- 0.10.0: 2015-04-13
 * Top Values aggregate support.
 * Upsert support
 * New Patch ops (init, append, nested patch, nested merge, test field present/missing)
- 0.9.0: 2015-03-05
 * Event Search Support
- 0.8.0: 2015-02-03
 * Timezone Support for Timeseries aggregations
- 0.7.0: 2014-12-16
 * Aggregations
- 0.6.0: 2014-12-11
 * Partial Updates
