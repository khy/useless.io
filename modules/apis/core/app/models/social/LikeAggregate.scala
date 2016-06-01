package models.core.social

case class LikeAggregate(
  resourceApi: String,
  resourceType: String,
  resourceId: String,
  count: Long
)
