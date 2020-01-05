package cps.forest

import scala.quoted._
import scala.quoted.matching._

import cps._
 
object IfTransform

  /**
   *'''
   * '{ _root_.cps.await[F,$ftType]($ft) } 
   *'''
   **/
  def run[F[_]:Type,T:Type](cpsCtx: TransformationContext[F,T], 
                               cond: Expr[Boolean], ifTrue: Expr[T], ifFalse: Expr[T]
                               )(given qctx: QuoteContext): CpsExprResult[F,T] =
     import qctx.tasty.{_, given}
     import util._
     import cpsCtx._
     val cR = Async.rootTransform(cond, asyncMonad, false)
     val tR = Async.rootTransform(ifTrue, asyncMonad, false)
     val fR = Async.rootTransform(ifFalse, asyncMonad, false)
     var isAsync = true

     val cnBuild = {
       if (!cR.haveAwait)
         if (!tR.haveAwait && !fR.haveAwait) 
            isAsync = false
            CpsChunkBuilder.sync(patternCode,asyncMonad)
         else
            new CpsChunkBuilder[F,T](asyncMonad) {
               override def create() = fromFExpr(
                '{ if ($cond) 
                     ${tR.cpsBuild.create().toExpr}
                   else 
                     ${fR.cpsBuild.create().toExpr} })
               override def append[A:quoted.Type](e:CpsChunk[F,A]) =
                     flatMapIgnore(e.toExpr)
            }
       else // (cR.haveAwait) 
         def condAsyncExpr() = cR.cpsBuild.create().toExpr
         if (!tR.haveAwait && !fR.haveAwait) 
           new CpsChunkBuilder[F,T](asyncMonad) {
             val mappedCond = '{ ${asyncMonad}.map(
                                 ${condAsyncExpr()}
                                )( c =>
                                   if (c) {
                                     ${ifTrue}
                                   } else {
                                     ${ifFalse}
                                   } 
                                )} 

              override def create() = fromFExpr(mappedCond)

              override def append[A:quoted.Type](e:CpsChunk[F,A]) =
                     flatMapIgnore(e.toExpr)
 
           }
         else
           new CpsChunkBuilder[F,T](asyncMonad) {
              override def create() = fromFExpr(
                   '{ ${asyncMonad}.flatMap(
                         ${condAsyncExpr()}
                       )( c =>
                           if (c) {
                              ${tR.cpsBuild.create().toExpr}
                           } else {
                              ${fR.cpsBuild.create().toExpr} 
                           } 
                        )
                    }) 
              override def append[A:quoted.Type](e:CpsChunk[F,A]) = 
                                  flatMapIgnore(e.toExpr)
           }
     }
     CpsExprResult[F,T](patternCode, cnBuild, patternType, isAsync)
     
