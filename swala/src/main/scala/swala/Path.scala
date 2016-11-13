package swala

case class Path(ref: String,
                parameters: Option[OperationParameter] = None,
                get: Option[Operation] = None,
                put: Option[Operation] = None,
                post: Option[Operation] = None,
                delete: Option[Operation] = None,
                options: Option[Operation] = None,
                patch: Option[Operation] = None,
                head: Option[Operation] = None)
