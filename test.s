
.text
 li $t0 10 #n is size of array. 
 sort: 
 li $s0 0
 li $s1 1
 li $s6 1
 
 loop4:
    lw $s2 $s0 0
    lw $s3 $s0 4
 			#Each time we load two integers and compare.
    slt $s4 $s2 $s3
    beq $s4 $s6 cont
			#Based on the values of integers we will either swap the values or we will proceed to next pair.
		swap:
	mov $t2 $s2
   mov $s2 $s3
			mov $s3 $t2

		cont:
			sw $s2 $s0 0
			sw $s3 $s0 4
			#After the operation we will store back the integer pair into the memory.
		addi $s1 $s1 1
			addi $s0 $s0 4
			bne $s1 $t0 loop4
			sub $t0 $t0 $s6
			bne $s6 $t0 sort 
