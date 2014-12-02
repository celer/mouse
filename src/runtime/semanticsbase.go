package mouse;

type SemanticsBase struct {
	Trace string;
	CurrentRule RuleResult;
}

func NewSemanticsBase() *SemanticsBase {
	s:=SemanticsBase{};
	return &s;
}

func (s *SemanticsBase) Init() {


}

//=====================================================================
//
//  Methods to be invoked from semantic actions.
//  They call back the parser to obtain details of the environment
//  in which the action was invoked.
//
//=====================================================================
//-------------------------------------------------------------------
//  Returns the left-hand side Phrase object.
//-------------------------------------------------------------------
func (this *SemanticsBase) LHS() *Phrase {
	return this.CurrentRule.LHS();
}

//-------------------------------------------------------------------
//  Returns the number of Phrase objects on the right-hand side.
//-------------------------------------------------------------------
func (this *SemanticsBase) RHSSize() int {
	return this.CurrentRule.RHSSize();
}

//-------------------------------------------------------------------
//  Returns the i-th right-hand side object, 0<=i<rhs<=rhsSize().
//  (The right-hand side objects are numbered starting with 0.)
//-------------------------------------------------------------------
func (this *SemanticsBase) RHS(i int) *Phrase {
	return this.CurrentRule.RHS(i);
}

//-------------------------------------------------------------------
//  Returns as one String the text represented
//  by the right-hand side objects numbered i through j-1,
//  where 0<=i<j<=rhsSize().
//  (The right-hand side objects are numbered starting with 0.)
//-------------------------------------------------------------------
func (this *SemanticsBase) RHSText(i int,j int) string {
	return this.CurrentRule.RHSText(i,j);
}
