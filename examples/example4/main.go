package main

import "flag";
import "../../src/runtime"
import "fmt"
import "os"

func main(){

	file:=flag.String("input","","File to read input from");
	ee:=flag.String("error","","Expected error string");
	rs:=flag.String("result","0.0","Expected result");
	

	
	flag.Parse();
	
	var result float64;
	fmt.Sscanf(*rs,"%f",&result);

	s,e:=mouse.NewFileSource(*file);
	if(e!=nil){
		panic(e);
	}
	p:=NewMyParser();
	res,err:=p.Parse(s);
	if(err!=nil){
		if(*ee!="" && err.Error()==*ee){
			fmt.Println("Got expected error:",err);
			os.Exit(0);
		} else {
			fmt.Println("Got unexpected error:",err);
			os.Exit(-1);
		}
	} else if(res==true) {
		if(p.sem.result==result){
			fmt.Println("Success, got:",p.sem.result);
			os.Exit(0);
		} else {
			fmt.Println("Failed got:",p.sem.result,"expected",result);
			os.Exit(-1);
		}
	} else {
		fmt.Println("Failed to parse input");
		os.Exit(-1);
	}

}
