package cloud.validation

sealed trait PropertyStatus
sealed trait Set extends PropertyStatus
sealed trait Unset extends PropertyStatus

sealed trait RuntimeStatus extends PropertyStatus
sealed trait HandlerStatus extends PropertyStatus
sealed trait HashKeyStatus extends PropertyStatus

type RuntimeSet = Set
type RuntimeUnset = Unset
type HandlerSet = Set
type HandlerUnset = Unset
type HashKeySet = Set
type HashKeyUnset = Unset
