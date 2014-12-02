# PEG Based Parser for golang

This is a runtime implementation of the Mouse PEG parser for golang. 

For more details see:

 * http://mousepeg.sourceforge.net/Manual.pdf
 * http://mousepeg.sourceforge.net/

From the author Roman R. Redziejowski

> Parsing Expression Grammar (PEG) is a new way to specify recursive-descent parsers with
> limited backtracking. The use of backtracking lifts the LL(1) restriction usually imposed by
> top-down parsers. In addition, PEG can define parsers with integrated lexing.
>
> Mouse is a tool to transcribe PEG into an executable parser written in Java. Unlike some
> existing PEG generators (e.g., Rats!), Mouse does not produce a storage-hungry ”packrat
> parser”, but a collection of transparent recursive procedures.
>
> An integral feature of Mouse is the mechanism for specifying semantics (also in Java). This
> makes Mouse a convenient tool if one needs an ad-hoc language processor. Being written in
> Java, the processor is operating-system independent

# Why use Mouse?

It generates easy to understand parsers which have integrated lexing, so a basic grammar
might look like so:

```
   Sum    = Number ("+" Number)* !_ {sum} ;
   Number = [0-9]+ {number} ;
``` 

Which when run through mouse generates a go source file containing a very readable
output like so:

```go
//=========================================================================
//
//  Parsing procedures
//
//=========================================================================
//=======================================================================
//  Sum = Number ("+" Number)* !_ {sum} ;
//=======================================================================
func (this *MyParser) Sum() bool {
	this.Begin("Sum");
	if (!this.Number()) { return this.Reject(); }
	for this.Sum_0() {};
	if (!this.AheadNot()){ return this.Reject(); }
	return this.Accept();
}

//---------------------------------------------------------------------
//  Sum_0 = "+" Number
//---------------------------------------------------------------------
func (this *MyParser) Sum_0() bool {
	this.Begin("");
	if (!this.NextRune(rune('+'))){ return this.RejectInner(); }
	if (!this.Number()) { return this.RejectInner(); }
	return this.AcceptInner();
}

//=======================================================================
//  Number = [0-9]+ {number} ;
//=======================================================================
func (this *MyParser) Number() bool {
	this.Begin("Number");
	if (!this.NextIn(rune('0'),rune('9'))){ return this.Reject(); }
	for this.NextIn(rune('0'),rune('9')) {};
	return this.Accept();
}
```

It has excellent error handling, making it very human friendly:

```
33+n:  After '33+': expected Number
44+4a: After '44+4': expected [0-9] or '+' or end of text
44-44: After '44': expected [0-9] or '+' or end of text
```

And it also is fairly easy to implement the supporting logic 
based upon the fired rules. Each rule can have an assoicated
method called when the rule is fired. The rule is results in some
number of right hand results, and a single left hand result. 

To gain a better understanding please see the mouse manual:

http://mousepeg.sourceforge.net/Manual.pdf

```go
//---------------------------------------------------------------------
//  Sum = Number ("+" Number)* !_
//---------------------------------------------------------------------
func (this *MySemantics) sum() {
      s:=0;
      for i:=0;i<this.RHSSize();i+=2 {
        r,ok:=this.RHS(i).Get().(int);
				if(ok){
					s+=r;
				}
			}
			this.result=s;
}

//---------------------------------------------------------------------
//  Number = [0-9]+
//---------------------------------------------------------------------
func (this *MySemantics) number() {
	var i int;
	fmt.Sscanf(this.LHS().Text(),"%d",&i);
	this.LHS().Put(i);
}
```

# Using it
	
See the /examples directory to get an idea of how to use it

